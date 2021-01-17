package de.elite12.musikbot.server.api;

import de.elite12.musikbot.server.api.dto.StatusUpdate;
import de.elite12.musikbot.server.services.StateUpdateService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/status")
public class StatusController {

    @Autowired
    private StateUpdateService stateUpdateService;

    @GetMapping()
    @ApiOperation(value = "Get the current Status")
    public StatusUpdate getStatus() {
        return stateUpdateService.getStateUpdate();
    }
}