package com.github.kjarosh.agh.pp.rest;

import com.github.kjarosh.agh.pp.rest.dto.LoadSimulationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.OperationDto;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * @author Kamil Jarosz
 */
@Slf4j
@Controller
public class LoadSimulatorController {
    @Autowired
    private GraphModificationController graphModificationController;

    @SneakyThrows
    @RequestMapping(method = RequestMethod.POST, path = "simulate_load")
    @ResponseBody
    public void simulateLoad(@RequestBody LoadSimulationRequestDto request) {
        List<OperationDto> operations = request.getOperations();
        if (operations.isEmpty()) {
            return;
        } else if (operations.size() == 1) {
            executeOperation(operations.get(0));
            return;
        }

        Instant now = Instant.now();
        Instant finish = now.plus(request.getTimeSpan());

        Duration delay = Duration.between(now, finish).dividedBy(operations.size());
        for (int i = 0; i < operations.size(); ++i) {
            OperationDto op = operations.get(i);
            executeOperation(op);

            if (i < operations.size() - 1) {
                Thread.sleep(delay.toMillis());
            }
        }
    }

    private void executeOperation(OperationDto op) {
        switch (op.getType()) {
            case ADD_EDGE:
                graphModificationController.addEdge(
                        op.getFromId().toString(),
                        op.getToId().toString(),
                        op.getPermissions().toString(),
                        op.getTrace(),
                        false);
                break;
            case REMOVE_EDGE:
                graphModificationController.removeEdge(
                        op.getFromId().toString(),
                        op.getToId().toString(),
                        op.getTrace(),
                        false);
                break;
            case SET_PERMS:
                graphModificationController.setPermissions(
                        op.getFromId().toString(),
                        op.getToId().toString(),
                        op.getPermissions().toString(),
                        op.getTrace(),
                        false);
                break;
        }
    }
}
