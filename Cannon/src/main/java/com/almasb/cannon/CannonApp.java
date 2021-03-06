/*
 * The MIT License (MIT)
 *
 * FXGL - JavaFX Game Library
 *
 * Copyright (c) 2015 AlmasB (almaslvl@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.almasb.cannon;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.ServiceType;
import com.almasb.fxgl.entity.control.Control;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityType;
import com.almasb.fxgl.input.Input;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.physics.CollisionHandler;
import com.almasb.fxgl.physics.PhysicsEntity;
import com.almasb.fxgl.physics.PhysicsWorld;
import com.almasb.fxgl.settings.GameSettings;
import com.almasb.fxgl.time.LocalTimer;
import com.almasb.fxgl.ui.UIFactory;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;

/**
 * @author Almas Baimagambetov (AlmasB) (almaslvl@gmail.com)
 */
public class CannonApp extends GameApplication {

    private enum Type implements EntityType {
        CANNON, BULLET, BASKET, BOUNDS
    }

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setIntroEnabled(false);
        settings.setMenuEnabled(true);
        settings.setShowFPS(false);
        settings.setTitle("FXGL Cannon");
        settings.setVersion("0.1dev");
    }

    @Override
    protected void initInput() {
        Input input = getInput();

        input.addAction(new UserAction("Shoot") {
            @Override
            protected void onActionBegin() {
                shoot();
            }
        }, MouseButton.PRIMARY);
    }

    @Override
    protected void initAssets() {

    }

    @Override
    protected void initGame() {
        initScreenBounds();
        initCannon();
        initBasket();
    }

    private void initScreenBounds() {
        PhysicsEntity top = new PhysicsEntity(Type.BOUNDS);
        top.setPosition(0, 0 - 100);
        top.setSceneView(new Rectangle(getWidth(), 100));

        PhysicsEntity bot = new PhysicsEntity(Type.BOUNDS);
        bot.setPosition(0, getHeight());
        bot.setSceneView(new Rectangle(getWidth(), 100));

        PhysicsEntity left = new PhysicsEntity(Type.BOUNDS);
        left.setPosition(0 - 100, 0);
        left.setSceneView(new Rectangle(100, getHeight()));

        PhysicsEntity right = new PhysicsEntity(Type.BOUNDS);
        right.setPosition(getWidth(), 0);
        right.setSceneView(new Rectangle(100, getHeight()));

        getGameWorld().addEntities(top, bot, left, right);
    }

    private Entity cannon;

    private void initCannon() {
        cannon = new Entity(Type.CANNON);
        cannon.setPosition(50, getHeight() - 30);
        cannon.setSceneView(new Rectangle(70, 30, Color.BROWN));
        cannon.addControl(new LiftControl());

        getGameWorld().addEntity(cannon);
    }

    private void initBasket() {
        PhysicsEntity basketLeft = new PhysicsEntity(Type.BASKET);
        basketLeft.setPosition(400, getHeight() - 300);
        basketLeft.setSceneView(new Rectangle(100, 300, Color.RED));

        PhysicsEntity basketRight = new PhysicsEntity(Type.BASKET);
        basketRight.setPosition(700, getHeight() - 300);
        basketRight.setSceneView(new Rectangle(100, 300, Color.RED));

        PhysicsEntity bottomLine = new PhysicsEntity(Type.BASKET);
        bottomLine.setPosition(500, getHeight());
        bottomLine.setSceneView(new Rectangle(200, 5, Color.TRANSPARENT));
        bottomLine.setCollidable(true);

        getGameWorld().addEntities(basketLeft, basketRight, bottomLine);
    }

    private void shoot() {
        PhysicsEntity bullet = new PhysicsEntity(Type.BULLET);
        bullet.setPosition(cannon.getPosition().add(70, 0));
        bullet.setSceneView(new Rectangle(25, 25, Color.BLUE));
        bullet.setBodyType(BodyType.DYNAMIC);
        bullet.setOnPhysicsInitialized(() -> {
            Point2D mousePosition = getInput().getMouse().getGameXY();

            bullet.setLinearVelocity(mousePosition.subtract(bullet.getPosition()).normalize().multiply(10));
        });

        FixtureDef fd = new FixtureDef();
        fd.density = 0.05f;

        bullet.setFixtureDef(fd);
        bullet.setCollidable(true);
        bullet.setExpireTime(Duration.seconds(4));

        getGameWorld().addEntity(bullet);
    }

    @Override
    protected void initPhysics() {
        PhysicsWorld physics = getPhysicsWorld();
        physics.addCollisionHandler(new CollisionHandler(Type.BULLET, Type.BASKET) {
            @Override
            protected void onCollisionBegin(Entity bullet, Entity basket) {
                bullet.removeFromWorld();
                score.set(score.get() + 1000);
            }
        });
    }

    private IntegerProperty score = new SimpleIntegerProperty(0);

    @Override
    protected void initUI() {
        Text scoreText = UIFactory.newText("", Color.BLACK, 24);
        scoreText.setTranslateX(550);
        scoreText.setTranslateY(100);
        scoreText.textProperty().bind(score.asString("Score: [%d]"));

        getGameScene().addUINode(scoreText);
    }

    @Override
    protected void onUpdate() {

    }

    private class LiftControl implements Control {

        private LocalTimer timer = getService(ServiceType.LOCAL_TIMER);
        private boolean goingUp = true;

        @Override
        public void onUpdate(Entity entity) {
            if (timer.elapsed(Duration.seconds(2))) {
                goingUp = !goingUp;
                timer.capture();
            }

            entity.translate(0, goingUp ? -2 : 2);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
