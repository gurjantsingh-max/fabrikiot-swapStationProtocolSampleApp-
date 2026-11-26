package com.intellicar.swapstation;

import com.intellicar.swapstation.data.live.heartbeat.HeartbeatSender;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;

@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class SwapStationApp_MembersInjector implements MembersInjector<SwapStationApp> {
  private final Provider<HeartbeatSender> heartbeatSenderProvider;

  private SwapStationApp_MembersInjector(Provider<HeartbeatSender> heartbeatSenderProvider) {
    this.heartbeatSenderProvider = heartbeatSenderProvider;
  }

  @Override
  public void injectMembers(SwapStationApp instance) {
    injectHeartbeatSender(instance, heartbeatSenderProvider.get());
  }

  public static MembersInjector<SwapStationApp> create(
      Provider<HeartbeatSender> heartbeatSenderProvider) {
    return new SwapStationApp_MembersInjector(heartbeatSenderProvider);
  }

  @InjectedFieldSignature("com.intellicar.swapstation.SwapStationApp.heartbeatSender")
  public static void injectHeartbeatSender(SwapStationApp instance,
      HeartbeatSender heartbeatSender) {
    instance.heartbeatSender = heartbeatSender;
  }
}
