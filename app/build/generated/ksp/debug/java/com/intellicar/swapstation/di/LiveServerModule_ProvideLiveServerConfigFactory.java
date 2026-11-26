package com.intellicar.swapstation.di;

import com.intellicar.swapstation.core.config.LiveServerConfig;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class LiveServerModule_ProvideLiveServerConfigFactory implements Factory<LiveServerConfig> {
  @Override
  public LiveServerConfig get() {
    return provideLiveServerConfig();
  }

  public static LiveServerModule_ProvideLiveServerConfigFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static LiveServerConfig provideLiveServerConfig() {
    return Preconditions.checkNotNullFromProvides(LiveServerModule.INSTANCE.provideLiveServerConfig());
  }

  private static final class InstanceHolder {
    static final LiveServerModule_ProvideLiveServerConfigFactory INSTANCE = new LiveServerModule_ProvideLiveServerConfigFactory();
  }
}
