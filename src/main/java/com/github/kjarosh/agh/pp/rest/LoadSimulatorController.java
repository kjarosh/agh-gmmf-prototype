package com.github.kjarosh.agh.pp.rest;

import com.github.kjarosh.agh.pp.rest.dto.LoadSimulationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.OperationDto;
import com.github.kjarosh.agh.pp.rest.error.OkException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

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
        }

        Exception rethrow = null;
        for (OperationDto op : operations) {
            try {
                executeOperation(op);
            } catch (OkException e) {
                // ignore
            } catch (Exception e) {
                log.error("Error while simulating load: {}", e.getMessage());
                rethrow = e;
            }
        }

        if (rethrow != null) {
            throw rethrow;
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
