package com.ohussar.simplelogin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.checkerframework.checker.units.qual.C;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.*;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(simplelogin.MODID)
public class simplelogin {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "simplelogin";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final Map<String, UUID> userPasswords = new HashMap<>();
    public static final List<String> usersAwaitingVerification = Collections.synchronizedList(new ArrayList<>());

    public static final Map<String, Vec3> userPositionPreLogin = new HashMap<>();

    public simplelogin(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);

        modEventBus.register(Networking.class);


        String read = Config.readFromFile("password");
        if(read.isEmpty()){
            UUID randomUUID = UUID.randomUUID();
            JsonElement element = JsonParser.parseString("{}");
            element.getAsJsonObject().addProperty("password", randomUUID.toString());
            Config.writeCustomFile("password", element.toString());
        }

    }
    public static JsonElement serverPasswordsJson;
    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        String passwords = Config.readFromFile("server_passwords");

        if(passwords.isEmpty()){
            JsonObject element = JsonParser.parseString("{}").getAsJsonObject();
            JsonArray array = new JsonArray();
            element.add("info", array);
            Config.writeCustomFile("server_passwords", element.toString());
            serverPasswordsJson = element;
        }else{
            JsonObject element = JsonParser.parseString(passwords).getAsJsonObject();
            JsonArray array = element.getAsJsonArray("info");
            serverPasswordsJson = element;
            for(int i = 0; i < array.size(); i++){
                JsonObject obj = array.get(i).getAsJsonObject();;

                for(Map.Entry<String, JsonElement> a : obj.asMap().entrySet()){
                    String username = a.getKey();
                    String uuid = a.getValue().getAsString();
                    userPasswords.put(username, UUID.fromString(uuid));
                }
            }

        }




    }

    @SubscribeEvent
    public void onPlayerJoin(EntityJoinLevelEvent event){
        if(event.getEntity() instanceof ServerPlayer player){
            boolean contains = userPasswords.containsKey(player.getName().getString());
            PacketDistributor.sendToPlayer(player, new Networking.PacketData(""));
            final String username = player.getName().getString();
            if(contains){
                usersAwaitingVerification.add(username);
                Vec3 startPos = new Vec3(player.getX(), player.getY(), player.getZ());
                player.setGameMode(GameType.SPECTATOR);
                userPositionPreLogin.put(username, startPos);
                Thread t = new Thread(() ->{
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    if(usersAwaitingVerification.contains(username)){
                        player.setPos(userPositionPreLogin.get(username));
                        player.setGameMode(GameType.SURVIVAL);
                        player.connection.disconnect(Component.literal(
                                "Tempo de verificação execedido. Isto tem relação ao login. Contate um administrador!"
                        ));
                        usersAwaitingVerification.remove(username);
                        userPositionPreLogin.remove(username);
                    }
                });
                t.setName("user-"+username+"-awaiting");
                t.start();
            }
        }
    }



}
