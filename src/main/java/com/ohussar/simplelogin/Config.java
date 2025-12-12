package com.ohussar.simplelogin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    public static void writeCustomFile(String filename, String content){
        Path gamedir = FMLPaths.GAMEDIR.get();
        Path file = gamedir.resolve(filename + ".json");
        try {
            Files.write(file, Collections.singletonList(content));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static String readFromFile(String filename){
        Path gamedir = FMLPaths.GAMEDIR.get();
        Path file = gamedir.resolve(filename + ".json");
        if(Files.exists(file)){
            try {
                return Files.readString(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return "";
    }


}
