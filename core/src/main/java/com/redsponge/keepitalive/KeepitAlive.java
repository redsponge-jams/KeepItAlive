package com.redsponge.keepitalive;

import com.redsponge.redengine.EngineGame;

public class KeepitAlive extends EngineGame {

    @Override
    public void init() {
        setScreen(new TutorialScreen(ga));
    }
}