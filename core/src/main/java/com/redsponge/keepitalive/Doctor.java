package com.redsponge.keepitalive;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.DelayedRemovalArray;
import com.redsponge.redengine.screen.components.Mappers;
import com.redsponge.redengine.screen.components.PositionComponent;
import com.redsponge.redengine.screen.components.SizeComponent;
import com.redsponge.redengine.utils.Logger;

public class Doctor extends Human {

    private Human wantToHelp;
    private PositionComponent helpPos;
    private SizeComponent helpSize;
    private float timeUntilNextHelp;

    public float syringes;
    private Texture syringe;

    private Texture syringeStraight, hat;
    private TextureRegion hatRegion;

    private static final DelayedRemovalArray<Human> takenHumans = new DelayedRemovalArray<>();

    public Doctor(SpriteBatch batch, ShapeRenderer shapeRenderer, int spawnX, int spawnY) {
        super(batch, shapeRenderer, spawnX, spawnY);
        speed = 50;
        maxHp = 10;
        hp = maxHp;
    }

    @Override
    public void loadAssets() {
        super.loadAssets();
        syringe = assets.get("syringe", Texture.class);
        syringeStraight = assets.get("syringeStraight", Texture.class);
        hat = assets.get("hat", Texture.class);
        hatRegion = new TextureRegion(hat);
    }

    @Override
    protected void generatePos() {
        if(wantToHelp != null) {
            wantToHelp.heal();
            takenHumans.removeValue(wantToHelp, true);
            syringes--;
            wantToHelp = null;
            timeUntilNextHelp = 3;
        }
        if(syringes >= 1 && timeUntilNextHelp <= 0) {
            tryFindNeedHelp();
        }
        if (wantToHelp == null) {
            super.generatePos();
        }
    }

    @Override
    public void additionalTick(float delta) {
        if(!isDead()) {
            timeUntilNextHelp -= delta;
            if (!isControlled) {
                syringes += delta / 2f;
                if (syringes > 3) syringes = 3;
            }

            if (wantToHelp != null) {
                wantedPos.set((int) (helpPos.getX() + helpSize.getX() / 2), (int) (helpPos.getY() + helpSize.getY() / 2f));
            }
        }
        super.additionalTick(delta);
    }

    private void tryFindNeedHelp() {
        Array<Human> humans = ((GameScreen)screen).getHumans();
        if(wantToHelp != null) {
            takenHumans.removeValue(wantToHelp, true);
        }
        wantToHelp = null;
        for (int i = 0; i < humans.size; i++) {
            if(humans.get(i) == this || takenHumans.contains(humans.get(i), true) || humans.get(i).isProtected()) continue;
            if(humans.get(i).isControlled) {
                PositionComponent pos = Mappers.position.get(humans.get(i));

                if(Vector2.dst2(this.pos.getX(), this.pos.getY(), pos.getX(), pos.getY()) < 50*50) {
                    continue;
                }
            }
            if(wantToHelp == null || wantToHelp.getHPRatio() > humans.get(i).getHPRatio()) {
                wantToHelp = humans.get(i);
            }
        }
        if(wantToHelp != null) {
            helpPos = Mappers.position.get(wantToHelp);
            helpSize = Mappers.size.get(wantToHelp);
            takenHumans.add(wantToHelp);
            Logger.log(this, "Found a new human!");
        }
    }

    @Override
    public void setControlled(boolean controlled) {
        if(wantToHelp != null) {
            takenHumans.removeValue(wantToHelp, true);
        }
        wantToHelp = null;
        super.setControlled(controlled);
    }

    @Override
    protected void die() {
        super.die();
        takenHumans.removeValue(wantToHelp, true);
        wantToHelp = null;
    }

    @Override
    public void additionalRender() {
        if(wantToHelp != null) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.begin(ShapeType.Filled);
            PositionComponent pos = Mappers.position.get(wantToHelp);
            SizeComponent size = Mappers.size.get(wantToHelp);
            Color a = Color.GOLD.cpy();
            Color b = Color.YELLOW.cpy();
            float dst = Vector2.dst2(this.pos.getX() + this.size.getX() / 2f, this.pos.getY() + this.size.getY() / 2f, pos.getX() + size.getX() / 2f, pos.getY() + size.getY() / 2f);
            float alpha;
            float minDst = 100;
            if(dst < minDst * minDst) {
                alpha = (minDst * minDst - dst) / (minDst * minDst);
            } else {
                alpha = 0;
            }
            a.set(a.r, a.g, a.b, alpha);
            b.set(b.r, b.g, b.b, alpha);
            shapeRenderer.rectLine(this.pos.getX() + this.size.getX() / 2f, this.pos.getY() + this.size.getY() / 2f, pos.getX() + size.getX() / 2f, pos.getY() + size.getY() / 2f, 1, a, b);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);

            float rotation = MathUtils.atan2(this.pos.getY() - pos.getY(), this.pos.getX() - pos.getX()) * MathUtils.radiansToDegrees;
            rotation += 90;
            batch.begin();
            batch.setColor(Color.GOLD);
            batch.draw(syringeStraight, this.pos.getX() + size.getX() / 2f - 3, this.pos.getY() + size.getY() / 2f, 3, 0, 6, syringeStraight.getHeight(), 1, 1, rotation, 0, 0, 6, syringeStraight.getHeight(), false, false);
            batch.end();
        }

        if(!isDead()) {
            batch.begin();
            batch.setColor(Color.WHITE);

            hatRegion.flip(!isFacingLeft, false);
            batch.draw(hatRegion, pos.getX() + 4, pos.getY() + size.getY());
            hatRegion.flip(!isFacingLeft, false);

            batch.setColor(isControlled ? Color.GREEN : Color.GOLD);
            for (int i = 0; i < (int) syringes; i++) {
                batch.draw(syringe, pos.getX() + 4 + i * 2, pos.getY() + size.getY() + 2, 8, 8);
            }
            batch.end();
        }
    }

    @Override
    public void removed() {
        super.removed();
        if(wantToHelp != null) {
            takenHumans.removeValue(wantToHelp, true);
        }
    }
}
