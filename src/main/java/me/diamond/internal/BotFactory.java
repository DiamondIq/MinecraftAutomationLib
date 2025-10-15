package me.diamond.internal;

import lombok.extern.slf4j.Slf4j;
import me.diamond.Bot;
import me.diamond.credentials.Credentials;
import me.diamond.credentials.MicrosoftCredentials;
import me.diamond.credentials.OfflineCredentials;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import net.raphimc.minecraftauth.step.msa.StepCredentialsMsaCode;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.auth.SessionService;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.factory.ClientNetworkSessionFactory;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class BotFactory {

    public static Bot createBot(Credentials credentials, InetSocketAddress serverAddress) {
        StepFullJavaSession.FullJavaSession fullSession;
        String username;

        MinecraftProtocol protocol;
        if (credentials instanceof MicrosoftCredentials microsoft) {
            fullSession = loginMicrosoft(microsoft);
            var profile = fullSession.getMcProfile();
            protocol = new MinecraftProtocol(new GameProfile(profile.getId(), profile.getName()), profile.getMcToken().getAccessToken());
            username = profile.getName();
        } else if (credentials instanceof OfflineCredentials(String username1)) {
            protocol = new MinecraftProtocol(username1);
            username = username1;
        } else {
            throw new IllegalArgumentException("Unsupported credentials type!");
        }

        ClientSession session = createSession(protocol, serverAddress);
        BotImpl bot = new BotImpl(session, credentials, serverAddress, username);

        // Keep JVM alive
        ScheduledExecutorService keepAlive = Executors.newSingleThreadScheduledExecutor();
        keepAlive.scheduleAtFixedRate(() -> {}, 0, 1, TimeUnit.HOURS);

        return bot;
    }

    public static ClientSession createSession(Credentials credentials, InetSocketAddress serverAddress) {
        MinecraftProtocol protocol = null;
        if (credentials instanceof MicrosoftCredentials microsoft) {
            StepFullJavaSession.FullJavaSession fullSession = loginMicrosoft(microsoft);
            var profile = fullSession.getMcProfile();
            protocol = new MinecraftProtocol(new GameProfile(profile.getId(), profile.getName()), profile.getMcToken().getAccessToken());
        } else if (credentials instanceof OfflineCredentials(String username)) {
            protocol = new MinecraftProtocol(username);
        }

        return createSession(protocol, serverAddress);
    }

    private static ClientSession createSession(MinecraftProtocol protocol, InetSocketAddress serverAddress) {
        var sessionService = new SessionService();
        var client = ClientNetworkSessionFactory.factory()
                .setRemoteSocketAddress(serverAddress)
                .setProtocol(protocol)
                .create();
        client.setFlag(MinecraftConstants.SESSION_SERVICE_KEY, sessionService);
        client.connect();
        return client;
    }

    private static StepFullJavaSession.FullJavaSession loginMicrosoft(MicrosoftCredentials credentials) {
        try {
            return MinecraftAuth.JAVA_CREDENTIALS_LOGIN.getFromInput(
                    MinecraftAuth.createHttpClient(),
                    new StepCredentialsMsaCode.MsaCredentials(credentials.email(), credentials.password())
            );
        } catch (Exception e) {
            throw new RuntimeException("Microsoft login failed", e);
        }
    }
}
