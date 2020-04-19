package com.redsponge.keepitalive;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.redsponge.redengine.screen.entity.ScreenEntity;

public class EntitySpawner extends ScreenEntity {

    private int spawnX, spawnY;
    private float time;

    public EntitySpawner(SpriteBatch batch, ShapeRenderer shapeRenderer, int spawnX, int spawnY) {
        super(batch, shapeRenderer);
        this.spawnX = spawnX;
        this.spawnY = spawnY;
    }

    @Override
    public void added() {
        pos.set(spawnX, spawnY);
        size.set(0, 0);
    }

    @Override
    public void additionalTick(float delta) {
        time += delta;
        if(time > 5) {
            spawnEntity();
            time -= 5;
        }
    }

    private void spawnEntity() {
        if(((GameScreen)screen).getHumans().size > 20) {
            return;
        }

        if(((GameScreen)screen).getTimeAlive() < 10) {
            ((GameScreen)screen).addHuman(new Human(batch, shapeRenderer, spawnX, spawnY));
        } else if(((GameScreen)screen).getTimeAlive() > 10 && ((GameScreen) screen).getTimeAlive() < 20) {
            if(MathUtils.random() > 0.3) {
                ((GameScreen)screen).addHuman(new Human(batch, shapeRenderer, spawnX, spawnY));
            } else {
                ((GameScreen)screen).addHuman(new Doctor(batch, shapeRenderer, spawnX, spawnY));
            }
        } else {
            if(MathUtils.randomBoolean()) {
                ((GameScreen)screen).addHuman(new Human(batch, shapeRenderer, spawnX, spawnY));
            } else {
                ((GameScreen)screen).addHuman(new Doctor(batch, shapeRenderer, spawnX, spawnY));
            }
        }
    }
}
