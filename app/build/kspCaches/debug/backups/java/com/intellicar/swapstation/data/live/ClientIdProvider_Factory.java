package com.intellicar.swapstation.data.live;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class ClientIdProvider_Factory implements Factory<ClientIdProvider> {
  private final Provider<Context> contextProvider;

  private ClientIdProvider_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ClientIdProvider get() {
    return newInstance(contextProvider.get());
  }

  public static ClientIdProvider_Factory create(Provider<Context> contextProvider) {
    return new ClientIdProvider_Factory(contextProvider);
  }

  public static ClientIdProvider newInstance(Context context) {
    return new ClientIdProvider(context);
  }
}
