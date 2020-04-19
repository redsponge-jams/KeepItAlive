package com.redsponge.keepitalive.texturepacker;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;

public class PackTextures {

    public static void main(String[] args) {
        TexturePacker.process("raw", "../assets/game/textures", "game");
    }

}
