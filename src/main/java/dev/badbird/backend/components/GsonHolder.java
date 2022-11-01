package dev.badbird.backend.components;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class GsonHolder {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Bean
    public Gson gson() {
        return gson;
    }
}
