package com.redsponge.keepitalive.desktop;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.redsponge.keepitalive.KeepitAlive;

/** Launches the desktop (LWJGL) application. */
public class DesktopLauncher {
	public static void main(String[] args) {
		createApplication();
	}

	private static LwjglApplication createApplication() {
		return new LwjglApplication(new KeepitAlive(), getDefaultConfiguration());
	}

	private static LwjglApplicationConfiguration getDefaultConfiguration() {
		LwjglApplicationConfiguration configuration = new LwjglApplicationConfiguration();
		configuration.title = "Experiment #31415 - The Takeover";
		configuration.width = 640;
		configuration.height = 360;
		for (int size : new int[] { 128, 64, 32, 16 }) {
			configuration.addIcon("icon" + size + ".png", FileType.Internal);
		}
		return configuration;
	}
}