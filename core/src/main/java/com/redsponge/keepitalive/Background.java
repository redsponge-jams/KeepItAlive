package com.redsponge.keepitalive;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.redsponge.redengine.screen.components.RenderRunnableComponent;
import com.redsponge.redengine.screen.entity.ScreenEntity;

public class Background extends ScreenEntity {


    private Texture grass;

    public Background(SpriteBatch batch, ShapeRenderer shapeRenderer) {
        super(batch, shapeRenderer);
    }

    @Override
    public void added() {
        pos.set(0, 0, -10);
        add(new RenderRunnableComponent(this::render));
    }

    @Override
    public void loadAssets() {
        grass = assets.get("grass", Texture.class);
    }

    private void render() {
        for (int i = 0; i < ((GameScreen) screen).getGameHeight(); i+=16) {
            for (int j = 0; j < ((GameScreen) screen).getGameWidth(); j+=16) {
                batch.draw(grass, j, i);
            }
        }
    }
}
