import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class Main extends JPanel implements ActionListener {
    // 0=floor, 1=wall, 2=clue, 3=key, 4=door, 5=ghost spawn, 6=cabinet, 7=calm pill, 8=flash charge
    private final int[][][] levels = {
            {
                    {1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                    {1, 0, 0, 0, 1, 0, 2, 7, 3, 1},
                    {1, 0, 1, 0, 0, 0, 1, 1, 0, 1},
                    {1, 2, 1, 1, 1, 0, 6, 0, 8, 4},
                    {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
            },
            {
                    {1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                    {1, 0, 0, 0, 0, 0, 1, 2, 4, 1},
                    {1, 0, 1, 1, 1, 0, 1, 1, 0, 1},
                    {1, 0, 6, 0, 5, 0, 3, 7, 8, 1},
                    {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
            },
            {
                    {1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                    {1, 4, 0, 0, 0, 1, 0, 2, 0, 1},
                    {1, 1, 1, 0, 1, 1, 0, 1, 0, 1},
                    {1, 0, 0, 6, 0, 7, 3, 1, 5, 1},
                    {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
            },
            {
                    {1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                    {1, 0, 0, 0, 1, 0, 0, 2, 4, 1},
                    {1, 0, 1, 0, 1, 0, 1, 1, 1, 1},
                    {1, 5, 1, 0, 0, 6, 8, 3, 0, 1},
                    {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
            },
            {
                    {1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                    {1, 0, 0, 0, 0, 0, 1, 0, 4, 1},
                    {1, 0, 1, 1, 1, 0, 1, 0, 1, 1},
                    {1, 2, 3, 5, 0, 0, 6, 7, 8, 1},
                    {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
            }
    };

    private int currentLevel = 0;
    private int[][] map = levels[currentLevel];
    private final int tileSize = 64;

    private final HashMap<String, BufferedImage> images = new HashMap<>();
    private final List<Ghost> ghosts = new ArrayList<>();

    private double px = 100;
    private double py = 100;
    private final double playerStartX = 100;
    private final double playerStartY = 100;
    private int foundClues = 0;
    private boolean facingRight = false;
    private boolean gameOverHandling = false;
    private boolean gameEnded = false;
    private boolean hasKey = false;
    private boolean hiddenInCabinet = false;
    private boolean showedLockedDoorHint = false;
    private boolean flashlightOn = true;
    private double battery = 100.0;
    private final double maxBattery = 100.0;
    private final double batteryDrainPerTick = 0.03;
    private double stamina = 100.0;
    private final double maxStamina = 100.0;
    private double sanity = 100.0;
    private final double maxSanity = 100.0;
    private boolean movingLeft = false;
    private boolean movingRight = false;
    private boolean movingUp = false;
    private boolean movingDown = false;
    private boolean sprintPressed = false;
    private boolean runningThisTick = false;
    private double shakeStrength = 0.0;
    private final Random random = new Random();
    private int calmPills = 0;
    private int flashCharges = 0;
    private int flashTicks = 0;
    private long frameTick = 0;

    private static class Ghost {
        double x;
        double y;
        double dirX = 1.0;
        double dirY = 0.0;
        int wanderTicks = 0;
        boolean seesPlayer = false;
        int stunTicks = 0;

        Ghost(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    public Main() {
        loadResources();
        initializeGhostsForCurrentLevel();
        setPreferredSize(new Dimension(map[0].length * tileSize, map.length * tileSize));

        Timer timer = new Timer(16, this);
        timer.start();

        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (gameEnded) {
                    return;
                }
                int keyCode = e.getKeyCode();
                if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A) {
                    movingLeft = true;
                    facingRight = false;
                }
                if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_D) {
                    movingRight = true;
                    facingRight = true;
                }
                if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) {
                    movingUp = true;
                }
                if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) {
                    movingDown = true;
                }
                if (keyCode == KeyEvent.VK_SHIFT) {
                    sprintPressed = true;
                }
                if (keyCode == KeyEvent.VK_SPACE) {
                    checkInteraction();
                }
                if (keyCode == KeyEvent.VK_F) {
                    flashlightOn = !flashlightOn;
                }
                if (keyCode == KeyEvent.VK_1) {
                    useCalmPill();
                }
                if (keyCode == KeyEvent.VK_2) {
                    useCameraFlash();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int keyCode = e.getKeyCode();
                if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A) {
                    movingLeft = false;
                }
                if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_D) {
                    movingRight = false;
                }
                if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) {
                    movingUp = false;
                }
                if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) {
                    movingDown = false;
                }
                if (keyCode == KeyEvent.VK_SHIFT) {
                    sprintPressed = false;
                }
            }
        });
    }

    private void loadResources() {
        String[] names = {"wall", "floor", "player", "ghost", "item", "key", "door", "cabinet", "pill", "flash"};
        for (String name : names) {
            try {
                images.put(name, ImageIO.read(new File("assets/custom/" + name + ".png")));
            } catch (Exception e) {
                System.out.println("assets/custom/" + name + ".png not found - fallback color used");
            }
        }
    }

    private boolean canMove(int x, int y) {
        int gx = x / tileSize;
        int gy = y / tileSize;

        if (gy < 0 || gy >= map.length || gx < 0 || gx >= map[0].length) {
            return false;
        }

        return map[gy][gx] != 1;
    }

    private void checkLevelExit() {
        int gx = (int) px / tileSize;
        int gy = (int) py / tileSize;

        if (map[gy][gx] != 4) {
            showedLockedDoorHint = false;
            return;
        }

        if (hiddenInCabinet) {
            return;
        }

        if (!hasKey) {
            if (!showedLockedDoorHint) {
                JOptionPane.showMessageDialog(this, "Door is locked. Find the key first.");
                showedLockedDoorHint = true;
            }
            return;
        }

        showedLockedDoorHint = false;
        hasKey = false;
        if (currentLevel < levels.length - 1) {
            currentLevel++;
            map = levels[currentLevel];
            px = playerStartX;
            py = playerStartY;
            hiddenInCabinet = false;
            initializeGhostsForCurrentLevel();
            System.out.println("Entered level " + (currentLevel + 1));
        } else {
            gameEnded = true;
            JOptionPane.showMessageDialog(this, "All stages cleared!");
        }
    }

    private void initializeGhostsForCurrentLevel() {
        ghosts.clear();
        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[y].length; x++) {
                if (map[y][x] == 5) {
                    Ghost ghost = new Ghost(x * tileSize + tileSize / 2.0, y * tileSize + tileSize / 2.0);
                    setRandomGhostDirection(ghost);
                    ghosts.add(ghost);
                }
            }
        }
    }

    private boolean canMoveGhost(double x, double y) {
        int gx = (int) x / tileSize;
        int gy = (int) y / tileSize;
        if (gy < 0 || gy >= map.length || gx < 0 || gx >= map[0].length) {
            return false;
        }
        return map[gy][gx] != 1;
    }

    private void updateGhosts() {
        double chaseSpeed = 1.9;
        double patrolSpeed = 1.05;
        for (Ghost ghost : ghosts) {
            if (ghost.stunTicks > 0) {
                ghost.stunTicks--;
                ghost.seesPlayer = false;
                continue;
            }

            double dx = px - ghost.x;
            double dy = py - ghost.y;
            double distance = Math.hypot(dx, dy);
            if (distance < 1) {
                continue;
            }

            ghost.seesPlayer = canGhostSeePlayer(ghost, 320.0);
            double stepX;
            double stepY;

            if (ghost.seesPlayer) {
                ghost.dirX = dx / distance;
                ghost.dirY = dy / distance;
                stepX = ghost.dirX * chaseSpeed;
                stepY = ghost.dirY * chaseSpeed;
            } else {
                if (ghost.wanderTicks <= 0) {
                    setRandomGhostDirection(ghost);
                    ghost.wanderTicks = 45 + random.nextInt(70);
                }
                ghost.wanderTicks--;
                stepX = ghost.dirX * patrolSpeed;
                stepY = ghost.dirY * patrolSpeed;
            }

            double nextX = ghost.x + stepX;
            double nextY = ghost.y + stepY;

            if (canMoveGhost(nextX, ghost.y)) {
                ghost.x = nextX;
            } else {
                setRandomGhostDirection(ghost);
                ghost.wanderTicks = 30 + random.nextInt(50);
            }
            if (canMoveGhost(ghost.x, nextY)) {
                ghost.y = nextY;
            } else {
                setRandomGhostDirection(ghost);
                ghost.wanderTicks = 30 + random.nextInt(50);
            }
        }
    }

    private void setRandomGhostDirection(Ghost ghost) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        ghost.dirX = Math.cos(angle);
        ghost.dirY = Math.sin(angle);
    }

    private boolean canGhostSeePlayer(Ghost ghost, double sightRange) {
        if (hiddenInCabinet) {
            return false;
        }
        double dx = px - ghost.x;
        double dy = py - ghost.y;
        double distance = Math.hypot(dx, dy);
        if (distance > sightRange || distance < 1.0) {
            return false;
        }

        double toPlayerX = dx / distance;
        double toPlayerY = dy / distance;
        double dot = toPlayerX * ghost.dirX + toPlayerY * ghost.dirY;
        if (dot < -0.2) {
            return false;
        }

        return hasLineOfSight(ghost.x, ghost.y, px, py);
    }

    private boolean hasLineOfSight(double fromX, double fromY, double toX, double toY) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        double distance = Math.hypot(dx, dy);
        if (distance < 1.0) {
            return true;
        }

        int steps = Math.max(1, (int) (distance / 8.0));
        for (int i = 1; i <= steps; i++) {
            double t = i / (double) steps;
            double sampleX = fromX + dx * t;
            double sampleY = fromY + dy * t;
            int gx = (int) sampleX / tileSize;
            int gy = (int) sampleY / tileSize;
            if (gy < 0 || gy >= map.length || gx < 0 || gx >= map[0].length) {
                return false;
            }
            if (map[gy][gx] == 1) {
                return false;
            }
        }
        return true;
    }

    private void checkGhostCollision() {
        for (Ghost ghost : ghosts) {
            double distance = Math.hypot(px - ghost.x, py - ghost.y);
            if (distance < 28) {
                onPlayerCaught();
                return;
            }
        }
    }

    private void onPlayerCaught() {
        if (gameOverHandling) {
            return;
        }
        gameOverHandling = true;
        JOptionPane.showMessageDialog(this, "Ghost got you! Restarting this stage.");
        px = playerStartX;
        py = playerStartY;
        hiddenInCabinet = false;
        stamina = maxStamina;
        sanity = Math.max(70.0, sanity);
        shakeStrength = 16.0;
        initializeGhostsForCurrentLevel();
        gameOverHandling = false;
    }

    private void checkInteraction() {
        int tx = (int) px / tileSize;
        int ty = (int) py / tileSize;

        if (hiddenInCabinet) {
            hiddenInCabinet = false;
            return;
        }

        if (map[ty][tx] == 2) {
            foundClues++;
            battery = Math.min(maxBattery, battery + 25.0);
            map[ty][tx] = 0;
            System.out.println("Clue found: " + foundClues);
        } else if (map[ty][tx] == 3) {
            hasKey = true;
            map[ty][tx] = 0;
            showedLockedDoorHint = false;
            System.out.println("Key obtained");
        } else if (map[ty][tx] == 6) {
            hiddenInCabinet = true;
            movingLeft = false;
            movingRight = false;
            movingUp = false;
            movingDown = false;
            sprintPressed = false;
        } else if (map[ty][tx] == 7) {
            calmPills++;
            map[ty][tx] = 0;
            System.out.println("Calm pill obtained: " + calmPills);
        } else if (map[ty][tx] == 8) {
            flashCharges++;
            map[ty][tx] = 0;
            System.out.println("Flash charge obtained: " + flashCharges);
        }
    }

    private void useCalmPill() {
        if (calmPills <= 0 || gameEnded) {
            return;
        }
        calmPills--;
        sanity = Math.min(maxSanity, sanity + 40.0);
        stamina = Math.min(maxStamina, stamina + 18.0);
        shakeStrength = Math.max(0.0, shakeStrength - 2.0);
    }

    private void useCameraFlash() {
        if (flashCharges <= 0 || gameEnded) {
            return;
        }
        flashCharges--;
        flashTicks = 6;

        for (Ghost ghost : ghosts) {
            double distance = Math.hypot(px - ghost.x, py - ghost.y);
            if (distance <= 230.0 && hasLineOfSight(px, py, ghost.x, ghost.y)) {
                ghost.stunTicks = Math.max(ghost.stunTicks, 75);
                double pushX = ghost.x - px;
                double pushY = ghost.y - py;
                double len = Math.max(1.0, Math.hypot(pushX, pushY));
                double pushedX = ghost.x + (pushX / len) * 26.0;
                double pushedY = ghost.y + (pushY / len) * 26.0;
                if (canMoveGhost(pushedX, ghost.y)) {
                    ghost.x = pushedX;
                }
                if (canMoveGhost(ghost.x, pushedY)) {
                    ghost.y = pushedY;
                }
            }
        }
    }

    private void updateBattery() {
        if (flashlightOn && battery > 0) {
            battery -= batteryDrainPerTick;
            if (battery <= 0) {
                battery = 0;
                flashlightOn = false;
            }
        }
    }

    private void updatePlayerMovementAndStamina() {
        if (hiddenInCabinet) {
            runningThisTick = false;
            stamina = Math.min(maxStamina, stamina + 0.55);
            return;
        }

        int xAxis = (movingRight ? 1 : 0) - (movingLeft ? 1 : 0);
        int yAxis = (movingDown ? 1 : 0) - (movingUp ? 1 : 0);
        boolean moving = xAxis != 0 || yAxis != 0;

        runningThisTick = false;
        double baseSpeed = 2.7;
        double sprintSpeed = 4.9;

        if (!moving) {
            stamina = Math.min(maxStamina, stamina + 0.42);
            return;
        }

        double speed = baseSpeed;
        if (sprintPressed && stamina > 5.0) {
            speed = sprintSpeed;
            runningThisTick = true;
            stamina = Math.max(0.0, stamina - 0.80);
        } else {
            stamina = Math.min(maxStamina, stamina + 0.20);
        }

        double length = Math.hypot(xAxis, yAxis);
        double moveX = (xAxis / length) * speed;
        double moveY = (yAxis / length) * speed;

        if (moveX > 0) facingRight = true;
        if (moveX < 0) facingRight = false;

        double nextX = px + moveX;
        double nextY = py + moveY;
        if (canMove((int) nextX, (int) py)) {
            px = nextX;
        }
        if (canMove((int) px, (int) nextY)) {
            py = nextY;
        }
        checkLevelExit();
    }

    private void updateSanityAndShake() {
        double nearestGhost = Double.MAX_VALUE;
        boolean watchedByGhost = false;
        for (Ghost ghost : ghosts) {
            double d = Math.hypot(px - ghost.x, py - ghost.y);
            nearestGhost = Math.min(nearestGhost, d);
            if (ghost.seesPlayer && d < 340.0) {
                watchedByGhost = true;
            }
        }

        if (hiddenInCabinet) {
            sanity = Math.min(maxSanity, sanity + 0.28);
        } else if (watchedByGhost || nearestGhost < 120.0) {
            double pressure = watchedByGhost ? 0.36 : 0.18;
            if (nearestGhost < 90.0) {
                pressure += 0.28;
            }
            sanity = Math.max(0.0, sanity - pressure);
        } else {
            sanity = Math.min(maxSanity, sanity + 0.12);
        }

        double targetShake = 0.0;
        if (nearestGhost < 150.0) {
            targetShake += (150.0 - nearestGhost) / 16.0;
        }
        if (runningThisTick && stamina < 35.0) {
            targetShake += 1.4;
        }
        if (sanity < 35.0) {
            targetShake += (35.0 - sanity) / 7.0;
        }
        shakeStrength = shakeStrength * 0.82 + targetShake * 0.18;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int shakeX = 0;
        int shakeY = 0;
        if (shakeStrength > 0.25) {
            shakeX = (int) Math.round((random.nextDouble() - 0.5) * shakeStrength * 2.0);
            shakeY = (int) Math.round((random.nextDouble() - 0.5) * shakeStrength * 2.0);
        }

        Graphics2D world = (Graphics2D) g2.create();
        world.translate(shakeX, shakeY);

        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                drawTile(world, map[i][j], j * tileSize, i * tileSize);
            }
        }

        if (images.get("player") != null) {
            int drawX = (int) px - 28;
            int drawY = (int) py - 30;
            int drawW = 56;
            int drawH = 72;
            if (facingRight) {
                world.drawImage(images.get("player"), drawX, drawY, drawW, drawH, null);
            } else {
                world.drawImage(images.get("player"), drawX + drawW, drawY, -drawW, drawH, null);
            }
        } else {
            world.setColor(Color.WHITE);
            world.fillOval((int) px - 15, (int) py - 15, 30, 30);
        }

        if (images.get("ghost") != null) {
            for (Ghost ghost : ghosts) {
                int gx = (int) ghost.x - tileSize / 2;
                int gy = (int) ghost.y - tileSize / 2;
                world.drawImage(images.get("ghost"), gx + 4, gy - 8, tileSize - 8, tileSize + 16, null);
            }
        }

        drawFlashlight(world);
        world.dispose();

        drawSanityFilter(g2);
        drawHud(g2);
    }

    private void drawSanityFilter(Graphics2D g2) {
        float panic = (float) (1.0 - sanity / maxSanity);
        if (panic <= 0.01f) {
            return;
        }

        int w = getWidth();
        int h = getHeight();
        int tintAlpha = Math.min(150, (int) (45 + panic * 95));
        g2.setColor(new Color(55, 18, 18, tintAlpha));
        g2.fillRect(0, 0, w, h);

        int edgeAlpha = Math.min(190, (int) (60 + panic * 130));
        RadialGradientPaint edge = new RadialGradientPaint(
                new Point((int) px, (int) py),
                (float) (Math.max(w, h) * (0.45 + (1.0 - panic) * 0.45)),
                new float[]{0f, 0.75f, 1f},
                new Color[]{
                        new Color(0, 0, 0, 0),
                        new Color(0, 0, 0, edgeAlpha / 2),
                        new Color(0, 0, 0, edgeAlpha)
                }
        );
        Paint prev = g2.getPaint();
        g2.setPaint(edge);
        g2.fillRect(0, 0, w, h);
        g2.setPaint(prev);

        if (panic > 0.35f) {
            int lineAlpha = Math.min(90, (int) (panic * 90));
            g2.setColor(new Color(255, 255, 255, lineAlpha));
            for (int y = (int) (frameTick % 7); y < h; y += 7) {
                g2.drawLine(0, y, w, y);
            }
        }
    }

    private void drawFlashlight(Graphics2D g2) {
        int w = getWidth();
        int h = getHeight();
        int cx = (int) px;
        int cy = (int) py;
        float lightStrength = flashlightOn ? (float) Math.max(0.20, battery / maxBattery) : 0.0f;
        boolean lowBattery = flashlightOn && battery > 0 && battery <= 20.0;
        if (lowBattery) {
            float pulse = (float) (0.80 + 0.20 * Math.sin(frameTick * 0.45));
            boolean blinkOff = ((frameTick / 4) % 7) == 0;
            lightStrength *= pulse;
            if (blinkOff) {
                lightStrength *= 0.18f;
            }
        }
        int radius = flashlightOn ? (int) (95 + 165 * lightStrength) : 52;

        RadialGradientPaint paint = new RadialGradientPaint(
                new Point(cx, cy),
                radius,
                new float[]{0f, 0.65f, 1f},
                new Color[]{
                        new Color(0, 0, 0, 0),
                        new Color(0, 0, 0, flashlightOn ? 190 : 235),
                        new Color(0, 0, 0, 250)
                }
        );

        Paint prev = g2.getPaint();
        g2.setPaint(paint);
        g2.fillRect(0, 0, w, h);
        g2.setPaint(prev);
    }

    private void drawHud(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRoundRect(10, 10, 240, 112, 12, 12);
        g2.setColor(Color.WHITE);
        g2.drawString("Battery: " + (int) battery + "%", 20, 32);
        g2.drawString("Stamina: " + (int) stamina + "%", 20, 50);
        g2.drawString("Sanity: " + (int) sanity + "%", 20, 68);
        g2.drawString("Flashlight [F]: " + (flashlightOn ? "ON" : "OFF"), 20, 86);
        g2.drawString("Key: " + (hasKey ? "YES" : "NO"), 20, 104);
    }

    private void drawTile(Graphics2D g2, int type, int x, int y) {
        String key = "floor";
        if (type == 1) key = "wall";
        else if (type == 2) key = "item";
        else if (type == 3) key = "key";
        else if (type == 4) key = "door";

        if (images.get(key) != null) {
            g2.drawImage(images.get(key), x, y, tileSize, tileSize, null);
            return;
        }

        if (type == 1) g2.setColor(Color.DARK_GRAY);
        else if (type == 2) g2.setColor(Color.YELLOW);
        else if (type == 3) g2.setColor(Color.ORANGE);
        else if (type == 4) g2.setColor(Color.BLUE);
        else g2.setColor(Color.BLACK);
        g2.fillRect(x, y, tileSize, tileSize);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        frameTick++;
        if (!gameEnded) {
            updatePlayerMovementAndStamina();
            updateGhosts();
            checkGhostCollision();
        }
        updateSanityAndShake();
        updateBattery();
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Java Horror Level System");
            Main panel = new Main();
            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
            panel.requestFocusInWindow();
        });
    }
}
