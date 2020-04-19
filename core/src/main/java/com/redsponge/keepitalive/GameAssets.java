package com.redsponge.keepitalive;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.redsponge.redengine.assets.Asset;
import com.redsponge.redengine.assets.AssetSpecifier;
import com.redsponge.redengine.assets.atlas.AtlasAnimation;
import com.redsponge.redengine.assets.atlas.AtlasFrame;

public class GameAssets extends AssetSpecifier {
    public GameAssets(AssetManager am) {
        super(am);
    }

    @Asset("game/syringe.png")
    private Texture syringe;

    @Asset("game/syringe_straight.png")
    private Texture syringeStraight;

    @Asset("game/takeover_arrow.png")
    private Texture takeoverArrow;

    @Asset("game/textures/game.atlas")
    private TextureAtlas gameTextures;

    @AtlasAnimation(atlas = "gameTextures", animationName = "human_walk", length = 3, playMode = Animation.PlayMode.LOOP)
    private Animation<TextureRegion> humanWalk;

    @AtlasAnimation(atlas = "gameTextures", animationName = "human_face", length = 5, playMode = Animation.PlayMode.NORMAL)
    private Animation<TextureRegion> humanFace;

    @AtlasAnimation(atlas = "gameTextures", animationName = "controlled", length = 4)
    private Animation<TextureRegion> controlledAnimation;

    @AtlasAnimation(atlas = "gameTextures", animationName = "protected", length = 4)
    private Animation<TextureRegion> protectedAnimation;

    @AtlasFrame(atlas = "gameTextures", frameName = "human_dead")
    private TextureRegion humanDead;

    @Asset("game/warn.png")
    private Texture warnTexture;
}
