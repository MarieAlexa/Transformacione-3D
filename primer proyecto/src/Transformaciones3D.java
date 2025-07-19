import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import javax.swing.*;
import java.util.Scanner;

public class Transformaciones3D implements GLEventListener {
    private float[][] objetoActual;
    private GLU glu = new GLU();

    public static void main(String[] args) {
        JFrame frame = new JFrame("Transformaciones 3D");
        GLCanvas canvas = new GLCanvas();
        Transformaciones3D escena = new Transformaciones3D();
        canvas.addGLEventListener(escena);
        frame.getContentPane().add(canvas);
        frame.setSize(800, 600);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Seleccione el objeto: 1. Pirámide, 2. Cubo, 3. Paralelepípedo");
        int objeto = sc.nextInt();
        switch (objeto) {
            case 1 -> objetoActual = getPiramide();
            case 2 -> objetoActual = getCubo();
            case 3 -> objetoActual = getParalelepipedo();
            default -> System.exit(0);
        }

        System.out.println("Transformación: 1. Traslación, 2. Rotación, 3. Escalamiento");
        int opcion = sc.nextInt();

        switch (opcion) {
            case 1 -> {
                System.out.print("dx: "); float dx = sc.nextFloat();
                System.out.print("dy: "); float dy = sc.nextFloat();
                System.out.print("dz: "); float dz = sc.nextFloat();
                traslacion(objetoActual, dx, dy, dz);
            }
            case 2 -> {
                System.out.print("Ángulo: "); float angulo = sc.nextFloat();
                System.out.print("Eje (x1 y1 z1): "); float x1 = sc.nextFloat(), y1 = sc.nextFloat(), z1 = sc.nextFloat();
                System.out.print("      (x2 y2 z2): "); float x2 = sc.nextFloat(), y2 = sc.nextFloat(), z2 = sc.nextFloat();
                rotacion(objetoActual, x1, y1, z1, x2, y2, z2, angulo);
            }
            case 3 -> {
                System.out.print("sx: "); float sx = sc.nextFloat();
                System.out.print("sy: "); float sy = sc.nextFloat();
                System.out.print("sz: "); float sz = sc.nextFloat();
                escalado(objetoActual, sx, sy, sz);
            }
        }

        System.out.println("Proyección: 1. glOrtho, 2. glFrustum, 3. gluPerspective");
        int proy = sc.nextInt();
        proyeccionElegida = proy;
    }

    int proyeccionElegida = 1;

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();

        // Aplicar proyección elegida
        switch (proyeccionElegida) {
            case 1 -> gl.glOrtho(-10, 10, -10, 10, -10, 10);
            case 2 -> gl.glFrustum(-1, 1, -1, 1, 2, 10);
            case 3 -> {
                gl.glMatrixMode(GL2.GL_PROJECTION);
                gl.glLoadIdentity();
                glu.gluPerspective(60, 1.0, 1.0, 20.0);
                gl.glMatrixMode(GL2.GL_MODELVIEW);
            }
        }

        gl.glBegin(GL2.GL_LINES);
        for (int i = 0; i < objetoActual.length; i++) {
            float[] p1 = objetoActual[i];
            float[] p2 = objetoActual[(i + 1) % objetoActual.length];
            gl.glVertex3f(p1[0], p1[1], p1[2]);
            gl.glVertex3f(p2[0], p2[1], p2[2]);
        }
        gl.glEnd();
        gl.glFlush();
    }

    @Override public void reshape(GLAutoDrawable d, int x, int y, int w, int h) {}
    @Override public void dispose(GLAutoDrawable d) {}

    

    public void traslacion(float[][] obj, float dx, float dy, float dz) {
        for (float[] p : obj) {
            p[0] += dx; p[1] += dy; p[2] += dz;
        }
    }

    public void escalado(float[][] obj, float sx, float sy, float sz) {
        for (float[] p : obj) {
            p[0] *= sx; p[1] *= sy; p[2] *= sz;
        }
    }

    public void rotacion(float[][] obj, float x1, float y1, float z1,
                         float x2, float y2, float z2, float angulo) {
        // Normalizar eje
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float mag = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= mag; dy /= mag; dz /= mag;

        float rad = (float) Math.toRadians(angulo);
        float cos = (float) Math.cos(rad), sin = (float) Math.sin(rad);

        for (int i = 0; i < obj.length; i++) {
            float[] p = obj[i];
            float x = p[0] - x1, y = p[1] - y1, z = p[2] - z1;

            float dot = dx * x + dy * y + dz * z;
            float crossX = dy * z - dz * y;
            float crossY = dz * x - dx * z;
            float crossZ = dx * y - dy * x;

            p[0] = x1 + dx * dot * (1 - cos) + x * cos + crossX * sin;
            p[1] = y1 + dy * dot * (1 - cos) + y * cos + crossY * sin;
            p[2] = z1 + dz * dot * (1 - cos) + z * cos + crossZ * sin;
        }
    }

    public float[][] getPiramide() {
        return new float[][] {
            {1, 0, -1}, {3, 0, -1}, {3, 0, -3}, {1, 0, -3}, {2, 2, -2},
            {1, 0, -1}, {2, 2, -2}, {3, 0, -1}, {3, 0, -3}, {2, 2, -2}, {1, 0, -3}
        };
    }

    public float[][] getCubo() {
        return new float[][] {
            {-4, 2, -1}, {-2, 2, -1}, {-2, 2, -3}, {-4, 2, -3}, {-4, 4, -1},
            {-2, 4, -1}, {-2, 4, -3}, {-4, 4, -3}, {-4, 2, -1}
        };
    }

    public float[][] getParalelepipedo() {
        return new float[][] {
            {-2, -2, 5}, {2, -2, 5}, {2, -2, 2}, {-2, -2, 2}, {-1, -1, 4},
            {1, -1, 4}, {1, -1, 3}, {-1, -1, 3}, {-2, -2, 5}
        };
    }
}









