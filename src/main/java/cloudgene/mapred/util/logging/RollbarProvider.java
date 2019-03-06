package cloudgene.mapred.util.logging;

import com.rollbar.notifier.config.ConfigBuilder;
import com.rollbar.notifier.config.ConfigProvider;

import cloudgene.mapred.Main;

public class RollbarProvider implements ConfigProvider {

	@Override
	public com.rollbar.notifier.config.Config provide(ConfigBuilder builder) {
		return builder.platform("java").codeVersion(Main.VERSION).handleUncaughtErrors(true).build();
	}
}