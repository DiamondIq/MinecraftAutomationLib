package me.diamond.credentials;

public sealed interface Credentials permits MicrosoftCredentials, OfflineCredentials {
}
