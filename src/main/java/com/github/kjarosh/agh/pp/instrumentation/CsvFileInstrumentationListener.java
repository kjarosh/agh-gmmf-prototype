package com.github.kjarosh.agh.pp.instrumentation;

import org.apache.commons.text.StringEscapeUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * @author Kamil Jarosz
 */
public class CsvFileInstrumentationListener implements InstrumentationListener {
    private static final NotificationFieldSerializer[] serializers = {
            n -> n.getTime().toString(),
            Notification::getThread,
            notification -> notification.getType().getValue(),
            Notification::getTrace,
            notification -> notification.getEventType().toString(),
            Notification::getSender,
            Notification::getOriginalSender,
    };

    private final Path target;
    private BufferedWriter writer;

    public CsvFileInstrumentationListener(Path target) {
        this.target = target;
    }

    @Override
    public void open() {
        try {
            writer = Files.newBufferedWriter(target, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    public void handle(Notification[] bulk, int size) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < size; ++i) {
            Notification n = bulk[i];
            for (NotificationFieldSerializer serializer : serializers) {
                buffer.append('"');
                buffer.append(StringEscapeUtils.escapeCsv(serializer.serialize(n)));
                buffer.append('"');
                buffer.append(',');
            }

            buffer.setCharAt(buffer.length() - 1, '\n');
        }

        try {
            writer.write(buffer.toString());
            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
