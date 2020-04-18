package com.redsponge.keepitalive;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.redsponge.redengine.assets.Asset;
import com.redsponge.redengine.assets.AssetSpecifier;

public class GameAssets extends AssetSpecifier {
    public GameAssets(AssetManager am) {
        super(am);
    }

    @Asset("game/syringe.png")
    private Texture syringe;
}
