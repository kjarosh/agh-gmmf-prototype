package com.github.kjarosh.agh.pp.rest;

import com.github.kjarosh.agh.pp.config.Config;
import com.github.kjarosh.agh.pp.rest.dto.BulkOperationDto;
import com.github.kjarosh.agh.pp.rest.dto.LoadSimulationRequestDto;
import com.github.kjarosh.agh.pp.rest.error.OkException;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Kamil Jarosz
 */
@Slf4j
@Controller
public class LoadSimulatorController {
    private static final ExecutorService executor =
            Executors.newFixedThreadPool(10, new ThreadFactoryBuilder()
                    .setNameFormat(Config.ZONE_ID + "-load-simulator-%d")
                    .build());

    @Autowired
    private GraphModificationController graphModificationController;

    @SneakyThrows
    @RequestMapping(method = RequestMethod.POST, path = "simulate_load")
    @ResponseBody
    public void simulateLoad(@RequestBody LoadSimulationRequestDto request) {
        List<BulkOperationDto> operations = request.getOperations();
        if (operations.isEmpty()) {
            return;
        }

        Exception rethrow = null;
        List<Future<?>> futures = new ArrayList<>();
        for (BulkOperationDto op : operations) {
            try {
                futures.add(executeOperation(op));
            } catch (OkException e) {
                // ignore
            } catch (Exception e) {
                log.error("Error while simulating load: {}", e.getMessage());
                rethrow = e;
            }
        }

        for (Future<?> future : futures) {
            future.get();
        }

        if (rethrow != null) {
            throw rethrow;
        }
    }

    private Future<?> executeOperation(BulkOperationDto op) {
        switch (op.getType()) {
            case ADD_EDGE:
                graphModificationController.addEdge(
                        op.getFromId().toString(),
                        op.getToId().toString(),
                        op.getPermissions().toString(),
                        op.getTrace(),
                        false);
                return null;
            case REMOVE_EDGE:
                graphModificationController.removeEdge(
                        op.getFromId().toString(),
                        op.getToId().toString(),
                        op.getTrace(),
                        false);
                return null;
            case SET_PERMS:
                return executor.submit(() -> graphModificationController.setPermissions(
                        op.getFromId().toString(),
                        op.getToId().toString(),
                        op.getPermissions().toString(),
                        op.getTrace(),
                        false));
        }
        return null;
    }
}
