package com.redsponge.keepitalive;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.DelayedRemovalArray;
import com.redsponge.redengine.assets.Fonts;
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

    private static final DelayedRemovalArray<Human> takenHumans = new DelayedRemovalArray<>();

    public Doctor(SpriteBatch batch, ShapeRenderer shapeRenderer) {
        super(batch, shapeRenderer);
        speed = 50;
        maxHp = 5;
        hp = 5;
    }

    @Override
    public void added() {
        super.added();
        pos.set(200, 100);
    }

    @Override
    public void loadAssets() {
        super.loadAssets();
        syringe = assets.get("syringe", Texture.class);
    }

    @Override
    protected void generatePos() {
        if(wantToHelp != null) {
            wantToHelp.heal();
            takenHumans.removeValue(wantToHelp, true);
            syringes--;
            wantToHelp = null;
        }
        if(syringes >= 1) {
            tryFindNeedHelp();
        }
        if (wantToHelp == null) {
            super.generatePos();
        }
    }

    @Override
    public void additionalTick(float delta) {
        if(!controlled) {
            syringes += delta / 2f;
            if(syringes > 3) syringes = 3;
        }

        if(wantToHelp != null) {
            wantedPos.set((int) (helpPos.getX() + helpSize.getX() / 2), (int) (helpPos.getY() + helpSize.getY()/ 2f));
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
    public void additionalRender() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(pos.getX() + 2, pos.getY() + size.getY() - 1, 4, 3);
        shapeRenderer.end();
        batch.begin();
        batch.setColor(Color.GOLD);
        for(int i = 1; i < syringes; i++) {
            batch.draw(syringe, pos.getX() + i * 2, pos.getY() + size.getY() + 2, 8, 8);
        }
        batch.end();
    }

    @Override
    public void removed() {
        super.removed();
        if(wantToHelp != null) {
            takenHumans.removeValue(wantToHelp, true);
        }
    }
}
