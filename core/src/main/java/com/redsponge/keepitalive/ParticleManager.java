package com.redsponge.keepitalive;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

public class ParticleManager implements Disposable {

    private Particle star;
    private Particle[] particles;

    public ParticleManager() {
        particles = new Particle[] {
                star = new Particle("game/particles/star.p")
        };
    }

    public void tick(float delta) {
        for (Particle particle : particles) {
            particle.tick(delta);
        }
    }

    public void render(SpriteBatch batch) {
        for (Particle particle : particles) {
            particle.render(batch);
        }
    }

    public Particle star() {
        return star;
    }

    @Override
    public void dispose() {
        for (Particle particle : particles) {
            particle.dispose();
        }
    }
}
