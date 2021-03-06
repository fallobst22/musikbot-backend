package de.elite12.musikbot.server.services;

import de.elite12.musikbot.server.api.dto.StatusUpdate;
import de.elite12.musikbot.server.data.entity.Song;
import de.elite12.musikbot.server.data.repository.SongRepository;
import de.elite12.musikbot.server.events.StateUpdateEvent;
import de.elite12.musikbot.server.events.entityevent.PostRemoveEvent;
import de.elite12.musikbot.server.events.entityevent.PostUpdateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;

@Service
public class StateUpdateService {

    @Autowired
    private SimpMessagingTemplate template;

    @Autowired
    private StateService stateService;

    @Autowired
    private SongRepository songRepository;

    private FluxSink<ApplicationEvent> debounce;

    public StateUpdateService() {
        Flux.<ApplicationEvent>create(fluxSink -> debounce = fluxSink)
                .sample(Duration.ofMillis(20))
                .subscribe(event -> {
                    StateService.StateData newState;
                    if (event instanceof StateUpdateEvent) newState = ((StateUpdateEvent) event).getNewState();
                    else newState = this.stateService.getState();

                    template.convertAndSend("/topic/state", this.getStateUpdate(newState));
                });
    }

    @EventListener
    public void onStateUpdate(StateUpdateEvent event) {
        debounce.next(event);
    }

    @EventListener(condition = "!#event.entity.played")
    public void onPlaylistChange(PostUpdateEvent<Song> event) {
        debounce.next(event);
    }

    @EventListener(condition = "!#event.entity.played")
    public void onPlaylistChange(PostRemoveEvent<Song> event) {
        debounce.next(event);
    }

    public StatusUpdate getStateUpdate() {
        return this.getStateUpdate(this.stateService.getState());
    }


    public StatusUpdate getStateUpdate(StateService.StateData stateData) {
        StatusUpdate st = new StatusUpdate();

        st.setStatus(stateData.getState().toString());
        st.setSongtitle(stateData.getSongTitle());
        st.setSonglink(stateData.getSongLink());
        st.setVolume(stateData.getVolume());

        int duration = 0;
        ArrayList<Song> list = new ArrayList<>(30);

        Iterable<Song> songs = this.songRepository.findByPlayedOrderBySort(false);

        for (Song s : songs) {
            duration += s.getDuration();
            list.add(s);
        }

        //Resort songs because if a song has been added just now the sort field hasnt been persisted to the database yet, and is therefore not respected by the repository query
        list.sort(Comparator.comparingLong(Song::getSort));

        st.setPlaylist(list);
        st.setPlaylistdauer(duration / 60);

        StateService.StateData.ProgressInfo progressInfo = stateData.getProgressInfo();

        if (progressInfo != null) {
            StatusUpdate.SongProgress sp = new StatusUpdate.SongProgress();
            sp.setStart(progressInfo.getStart());
            sp.setCurrent(Instant.now());
            sp.setDuration(progressInfo.getDuration());
            sp.setPaused(progressInfo.isPaused());
            sp.setPrepausedDuration(progressInfo.getPrepausedDuration());
            st.setProgress(sp);
        }

        return st;
    }
}
