package it.reactive.muskel.server.config;

import it.reactive.muskel.executor.NamedMuskelExecutorService;
import it.reactive.muskel.internal.executor.impl.MultipleMuskelExecutorService;
import it.reactive.muskel.internal.executor.local.LocalNamedMuskelExecutorService;
import it.reactive.muskel.utils.MuskelExecutorUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class LocalExecutorsConfiguration {

    private static final String COMMASEPARATED_REGEX = Pattern.quote(",");

    private static final String DOT_REGEX = Pattern.quote(":");

    @Value("${localexecutors:" + MuskelExecutorUtils.DEFAULT_LOCAL_NAME + "}")
    private String threadPoolNames;

    @SuppressWarnings("resource")
    @Bean
    public NamedMuskelExecutorService getLocalNamedMuskelExecutorService() {

	List<? extends NamedMuskelExecutorService> elements = Optional
		.ofNullable(threadPoolNames)
		.map(c -> c.split(COMMASEPARATED_REGEX))
		.map(current -> Arrays.stream(current)
			.map(currentString -> buildFromString(currentString))
			.collect(Collectors.toList())).orElse(null);

	return new MultipleMuskelExecutorService().addAll(elements);
    }

    protected LocalNamedMuskelExecutorService buildFromString(
	    String configuration) {
	int port = Runtime.getRuntime().availableProcessors();
	String name = configuration;
	String[] mapping = configuration.split(DOT_REGEX);
	if (mapping.length == 2) {
	    name = mapping[0];
	    port = Integer.parseInt(mapping[1]);
	}

	return buildFromString(name, port);
    }

    protected LocalNamedMuskelExecutorService buildFromString(String name,
	    int poolSize) {
	log.info("Creating MuskelExecutorService with name {} and poolsize {}",
		name, poolSize);
	return new LocalNamedMuskelExecutorService(poolSize, name) {

	    private static final long serialVersionUID = 1L;

	    @Override
	    public void close() {
		// Evito che venga chiamata la close
	    }
	};
    }
}
