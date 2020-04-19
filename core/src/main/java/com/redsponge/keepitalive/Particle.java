package com.redsponge.keepitalive;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool.PooledEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.DelayedRemovalArray;
import com.badlogic.gdx.utils.Disposable;

public class Particle implements Disposable {

    private String file;
    private ParticleEffect effect;
    private ParticleEffectPool pool;

    private DelayedRemovalArray<PooledEffect> effects;

    public Particle(String file) {
        this.file = file;
        this.effect = new ParticleEffect();
        this.effects = new DelayedRemovalArray<>();
        effect.load(Gdx.files.internal(file), Gdx.files.internal("game/particles"));
        pool = new ParticleEffectPool(effect, 1, 6);
    }

    public void spawn(float x, float y) {
        PooledEffect eff = pool.obtain();
        eff.setPosition(x, y);
        effects.add(eff);
    }

    public void tick(float delta) {
        for (int i = 0; i < effects.size; i++) {
            effects.get(i).update(delta);
            if(effects.get(i).isComplete()) {
                effects.removeIndex(i);
            }
        }
    }

    public void render(SpriteBatch batch) {
        for (int i = 0; i < effects.size; i++) {
            effects.get(i).draw(batch);
        }
    }

    @Override
    public void dispose() {
        for (PooledEffect pooledEffect : effects) {
            pooledEffect.free();
        }
        effects.clear();
        effect.dispose();
    }
}
