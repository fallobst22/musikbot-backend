package de.elite12.musikbot.server.api.v2;

import de.elite12.musikbot.server.api.dto.createSongResponse;
import de.elite12.musikbot.server.data.entity.Guest;
import de.elite12.musikbot.server.data.entity.LockedSong;
import de.elite12.musikbot.server.data.entity.Song;
import de.elite12.musikbot.server.data.entity.User;
import de.elite12.musikbot.server.data.repository.GuestRepository;
import de.elite12.musikbot.server.data.repository.LockedSongRepository;
import de.elite12.musikbot.server.data.repository.SongRepository;
import de.elite12.musikbot.server.exception.UnauthorizedException;
import de.elite12.musikbot.server.services.JWTUserService;
import de.elite12.musikbot.server.services.SongService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RequestMapping("/v2/songs")
@RestController
public class SongsController {

    @Autowired
	private SongService songservice;

    @Autowired
    private SongRepository songrepository;

    @Autowired
    private LockedSongRepository lockedsongrepository;

    @Autowired
    private JWTUserService jwtUserService;

    @Autowired
    private GuestRepository guestRepository;

    private static final Logger logger = LoggerFactory.getLogger(SongsController.class);


    @GetMapping(path = "{ids}", produces = {"application/json"})
    @Operation(summary = "Get Songs")
    @ApiResponses({@ApiResponse(responseCode = "404", description = "One of the requested Songs could not been found")})
    public ResponseEntity<de.elite12.musikbot.server.data.entity.Song[]> getSong(@Parameter(description = "Comma-seperated List of Song Ids to get") @PathVariable String ids) {
        String[] a = ids.split(",");
        de.elite12.musikbot.server.data.entity.Song[] r = new de.elite12.musikbot.server.data.entity.Song[a.length];
        try {
            for (int i = 0; i < a.length; i++) {
                long id = Long.parseLong(a[i]);
                r[i] = songrepository.findById(id).orElseThrow();
            }
        } catch (NumberFormatException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (NoSuchElementException e) {
        	return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(r, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('admin')")
    @DeleteMapping(path = "{ids}", produces = {"application/json"})
    @Operation(summary = "Delete Songs", description = "Requires Admin permissions.")
    @ApiResponses({@ApiResponse(responseCode = "404", description = "One of the requested Songs could not been found")})
    public ResponseEntity<Object> deleteSong(@Parameter(description = "Comma-seperated List of Song Ids to get") @PathVariable String ids, @Parameter(description = "If the Songs should be locked in addition to being deleted") @RequestParam(value = "lock", required = false) Boolean lock) {
        String[] a = ids.split("/");
        lock = lock != null && lock;
        try {
            ArrayList<Song> songs = new ArrayList<>();
            for (String b : a) {
                long id = Long.parseLong(b);
                Optional<Song> song = songrepository.findById(id);
                if (song.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                if (song.get().isPlayed()) return new ResponseEntity<>(HttpStatus.FORBIDDEN);
                
                songs.add(song.get());
            }

            for(Song song: songs) {
                if (lock) {
                    LockedSong ls = new LockedSong();
                    ls.setTitle(song.getTitle());
                    ls.setUrl(song.getLink());

                    lockedsongrepository.save(ls);
                }

                songrepository.delete(song);
            }

            logger.info(String.format("Songs %s by %s: %s", lock ? "deleted and locked" : "deleted", SecurityContextHolder.getContext().getAuthentication().getName(), Arrays.toString(a)));
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (NumberFormatException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping(path = "", consumes = {"text/plain"}, produces = {"application/json"})
    @Operation(summary = "Add a Song")
    public ResponseEntity<createSongResponse> createSong(@Parameter(description = "The URL of the Song to add") @RequestBody(required = false) String url, @RequestHeader(name = "X-Guest-Token", required = false) String guestHeader) {
        User u = null;
        Object credentials = SecurityContextHolder.getContext().getAuthentication().getCredentials();

        if (credentials instanceof Jwt) {
            u = jwtUserService.loadUserFromJWT((Jwt) credentials);
        }

        if (url == null) {
            return new ResponseEntity<>(new createSongResponse(false, false, "Songlink kann nicht leer sein"), HttpStatus.OK);
        }

        Guest guest;
        if (guestHeader != null && !guestHeader.isEmpty()) {
            Optional<Guest> optionalGuest = this.guestRepository.findByToken(guestHeader);
            if (optionalGuest.isEmpty()) {
                throw new UnauthorizedException("Guest Token invalid");
            }
            guest = optionalGuest.get();
        } else {
            guest = new Guest();
            guest.setIdentifier(UUID.randomUUID().toString());
            guest.setToken(UUID.randomUUID().toString());
            this.guestRepository.save(guest);
        }

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("X-GUEST-TOKEN", guest.getToken());

        createSongResponse createSongResponse = songservice.addSong(url, u, guest);
        return new ResponseEntity<>(createSongResponse, responseHeaders, HttpStatus.OK);
    }


    @PreAuthorize("hasRole('admin')")
    @PutMapping(path = "{ids}")
    @Operation(summary = "Sort Songs", description = "Requires Admin permissions.")
    public ResponseEntity<Object> sortsong(@Parameter(description = "The ID of the Song to resort") @PathVariable("ids") String sid, @Parameter(description = "The ID of the Song after which the Song should be sorted") @RequestBody(required = false) String prev) {
        try {
            long id = Long.parseLong(sid);
            long pr;
            try {
                pr = Long.parseLong(prev);
            } catch (NumberFormatException e) {
                pr = -1;
            }
            long low = Long.MAX_VALUE;
            
            Iterable<de.elite12.musikbot.server.data.entity.Song> songs = songrepository.findByPlayedOrderBySort(false);
            Iterator<de.elite12.musikbot.server.data.entity.Song> iterator = songs.iterator();
            de.elite12.musikbot.server.data.entity.Song cs;
            if (songs.iterator().hasNext()) {
            	cs = songs.iterator().next();
                low = cs.getSort();
            }
            if (pr == -1) {
                pr = low - 1;
            } else {
                pr = songrepository.findById(pr).orElseThrow().getSort();
            }
            
            de.elite12.musikbot.server.data.entity.Song s = songrepository.findById(id).orElseThrow();
            if(s.isPlayed()) return  new ResponseEntity<>(HttpStatus.FORBIDDEN);
            s.setSort(pr +1);
            songrepository.save(s);
            do {
            	cs = iterator.next();
                if (cs.getId() != id) {
                    if (cs.getSort() == pr + 1) {
                        low++;
                    }
                    if (cs.getSort() != low) {
                        cs.setSort(low);
                        songrepository.save(cs);
                    }
                    low++;
                }
            } while (iterator.hasNext());

            logger.info(String.format("Playlist sorted by %s", SecurityContextHolder.getContext().getAuthentication().getName()));
        } catch (NumberFormatException | NoSuchElementException e) {
        	return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
