package dev.badbird.backend.components;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.badbird.backend.object.Location;
import dev.badbird.backend.object.LocationSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class GsonHolder {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting()
            .registerTypeAdapter(Location.class, new LocationSerializer()).create();

    @Bean
    public Gson gson() {
        return gson;
    }
}
