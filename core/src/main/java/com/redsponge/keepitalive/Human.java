package com.redsponge.keepitalive;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.redsponge.redengine.screen.components.RenderRunnableComponent;
import com.redsponge.redengine.screen.entity.ScreenEntity;
import com.redsponge.redengine.utils.IntVector2;

public class Human extends ScreenEntity {

    protected boolean isControlled;
    protected IntVector2 wantedPos;
    protected float speed = 40;

    private boolean isProtected;

    protected float hp;
    private boolean isDead;
    protected float maxHp = 20;

    private int spawnX, spawnY;

    private Animation<TextureRegion> walkAnimation;
    private Animation<TextureRegion> faceAnimation;
    private Animation<TextureRegion> controlledAnimation;
    private Animation<TextureRegion> protectedAnimation;

    private float time;
    private boolean isFacingLeft;
    private TextureRegion deathFrame;
    private float deadTime;

    public Human(SpriteBatch batch, ShapeRenderer shapeRenderer, int spawnX, int spawnY) {
        super(batch, shapeRenderer);
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        wantedPos = new IntVector2();
        hp = maxHp;
    }

    @Override
    public void loadAssets() {
        walkAnimation = assets.getAnimation("humanWalk");
        faceAnimation = assets.getAnimation("humanFace");
        controlledAnimation = assets.getAnimation("controlledAnimation");
        protectedAnimation = assets.getAnimation("protectedAnimation");
        deathFrame = assets.getTextureRegion("humanDead");
    }

    @Override
    public void added() {
        pos.set(spawnX, spawnY);
        size.set(16, 16);
        add(new RenderRunnableComponent(this::render));
        generatePos();
    }

    protected void generatePos() {
        wantedPos.set((int) MathUtils.random(((GameScreen)screen).getGameWidth()), MathUtils.random(((GameScreen) screen).getGameHeight()));
    }

    @Override
    public void additionalTick(float delta) {
        time += delta;
        if(isDead) {
            deadTime -= delta;
            vel.set(0, 0);
            if(deadTime <= 0) {
                remove();
            }
            return;
        }
        if(hp <= 0) {
            die();
        }
        if(isControlled) {
            if(vel.getX() != 0) {
                isFacingLeft = vel.getX() < 0;
            }
            hp -= delta;
            return;
        } else {
            hp += delta / 2f;
            if(hp > maxHp) {
                hp = maxHp;
            }
        }
        double angle = Math.atan2(wantedPos.y - pos.getY(), wantedPos.x - pos.getX());
        double vx = Math.cos(angle);
        double vy = Math.sin(angle);
        vel.set((float) (vx * speed), (float) (vy * speed));
        if(Vector2.dst2(pos.getX() + size.getX() / 2f, pos.getY() + size.getY() / 2f, wantedPos.x, wantedPos.y) < 20 * 20) {
            generatePos();
        }
        isFacingLeft = vel.getX() < 0;
    }

    private void die() {
        isDead = true;
        ((GameScreen)screen).getHumans().removeValue(this, true);
        deadTime = 2;
    }

    public void virusLeft() {
        hp /= 1.25f;
        if(hp < 0.1f) {
            hp = 0;
        }
    }

    public void setControlled(boolean controlled) {
        this.isControlled = controlled;
    }

    private void render() {
        batch.setColor(Color.WHITE);
        if(isDead) {
            batch.setColor(new Color(1, 1, 1, deadTime / 2f));
            batch.draw(deathFrame, pos.getX(), pos.getY(), 16, 16);
            return;
        }
        TextureRegion reg = walkAnimation.getKeyFrame(time);
        reg.flip(isFacingLeft, false);
        batch.draw(walkAnimation.getKeyFrame(time), pos.getX(), pos.getY(), 16, 16);
        reg.flip(isFacingLeft, false);
        int faceIndex = (int) (MathUtils.map(0, 1, 0, 4, 1 - getHPRatio()));
        TextureRegion faceRegion = faceAnimation.getKeyFrame(faceIndex * 0.1f);
        faceRegion.flip(isFacingLeft, false);
        batch.draw(faceAnimation.getKeyFrame(faceIndex * 0.1f), pos.getX(), pos.getY(), 16, 16);
        faceRegion.flip(isFacingLeft, false);

        if(isControlled) {
            TextureRegion controlledRegion = controlledAnimation.getKeyFrame(time);
            controlledRegion.flip(isFacingLeft, false);
            batch.draw(controlledRegion, pos.getX(), pos.getY(), 16, 16);
            controlledRegion.flip(isFacingLeft, false);
        } else if(isProtected) {
            TextureRegion protectedRegion = protectedAnimation.getKeyFrame(time);
            protectedRegion.flip(isFacingLeft, false);
            batch.draw(protectedRegion, pos.getX(), pos.getY(), 16, 16);
            protectedRegion.flip(isFacingLeft, false);
        }
    }

    public boolean isDead() {
        return isDead;
    }

    public boolean isProtected() {
        return isProtected;
    }

    public void setProtected(boolean aProtected) {
        isProtected = aProtected;
    }

    public float getHPRatio() {
        return hp / maxHp;
    }

    protected float getHP() {
        return hp;
    }

    protected void heal() {
        if(hp > 9) {
            isProtected = true;
            hp = maxHp;
        } else {
            hp += 3;
            if (hp > maxHp) {
                hp = maxHp;
            }
        }
        if(isControlled) {
            notifyScreen(Notifications.CONTROLLED_HEALED);
            isControlled = false;
        }

    }

    public void injectBad() {
        if(isProtected) {
            isProtected = false;
        }
        else {
            hp -= 3;
        }
    }

    @Override
    public void removed() {
        ((GameScreen)screen).getHumans().removeValue(this, true);
    }
}
