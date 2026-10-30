import javax.swing.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

abstract class FiguraSVG {
    protected Color fill = null;
    protected Color stroke = Color.BLACK;
    protected float strokeWidth = 1.0f;
    public void configurarEstilo(Graphics2D g2d) { g2d.setStroke(new BasicStroke(strokeWidth)); }
    public abstract void dibujar(Graphics2D g2d);
}

class Linea extends FiguraSVG {
    double x1, y1, x2, y2;
    public Linea(double x1, double y1, double x2, double y2) { this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2; }
    public void dibujar(Graphics2D g2d) { configurarEstilo(g2d); g2d.setColor(stroke); g2d.draw(new Line2D.Double(x1, y1, x2, y2)); }
}

class Circulo extends FiguraSVG {
    double cx, cy, r;
    public Circulo(double cx, double cy, double r) { this.cx = cx; this.cy = cy; this.r = r; }
    public void dibujar(Graphics2D g2d) {
        configurarEstilo(g2d); Ellipse2D forma = new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
        if (fill != null) { g2d.setColor(fill); g2d.fill(forma); }
        g2d.setColor(stroke); g2d.draw(forma);
    }
}

class Elipse extends FiguraSVG {
    double cx, cy, rx, ry;
    public Elipse(double cx, double cy, double rx, double ry) { this.cx = cx; this.cy = cy; this.rx = rx; this.ry = ry; }
    public void dibujar(Graphics2D g2d) {
        configurarEstilo(g2d); Ellipse2D forma = new Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2);
        if (fill != null) { g2d.setColor(fill); g2d.fill(forma); }
        g2d.setColor(stroke); g2d.draw(forma);
    }
}

class Rectangulo extends FiguraSVG {
    double x, y, width, height;
    public Rectangulo(double x, double y, double w, double h) { this.x = x; this.y = y; this.width = w; this.height = h; }
    public void dibujar(Graphics2D g2d) {
        configurarEstilo(g2d); Rectangle2D forma = new Rectangle2D.Double(x, y, width, height);
        if (fill != null) { g2d.setColor(fill); g2d.fill(forma); }
        g2d.setColor(stroke); g2d.draw(forma);
    }
}

class Polilinea extends FiguraSVG {
    Path2D.Double path = new Path2D.Double();
    public Polilinea(String points) {
        if (points == null || points.isEmpty()) return;
        String[] pares = points.trim().split("\\s+");
        boolean primero = true;
        for (String par : pares) {
            String[] coords = par.split(",");
            if (coords.length == 2) {
                double x = Double.parseDouble(coords[0]), y = Double.parseDouble(coords[1]);
                if (primero) { path.moveTo(x, y); primero = false; } else { path.lineTo(x, y); }
            }
        }
    }
    public void dibujar(Graphics2D g2d) {
        configurarEstilo(g2d);
        if (fill != null) { g2d.setColor(fill); path.closePath(); g2d.fill(path); }
        g2d.setColor(stroke); g2d.draw(path);
    }
}

class Texto extends FiguraSVG {
    double x, y; String contenido, fontFamily; int fontSize;
    public Texto(double x, double y, String contenido, String fontFamily, int fontSize) {
        this.x = x; this.y = y; this.contenido = contenido;
        this.fontFamily = fontFamily != null ? fontFamily : "SansSerif";
        this.fontSize = fontSize > 0 ? fontSize : 12;
    }
    public void dibujar(Graphics2D g2d) {
        g2d.setFont(new Font(fontFamily, Font.PLAIN, fontSize));
        g2d.setColor(fill != null ? fill : Color.BLACK); g2d.drawString(contenido, (float)x, (float)y);
    }
}

class LienzoSVG extends JPanel {
    private List<FiguraSVG> figuras = new ArrayList<>();
    public void setFiguras(List<FiguraSVG> nuevasFiguras) { this.figuras = nuevasFiguras; repaint(); }
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (FiguraSVG fig : figuras) fig.dibujar(g2d);
    }
}

public class VisorSVG extends JFrame {
    private LienzoSVG lienzo;
    public VisorSVG() {
        setTitle("Visor SVG DOM"); setSize(800, 600); setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        lienzo = new LienzoSVG(); lienzo.setBackground(Color.WHITE); add(lienzo, BorderLayout.CENTER);
        JButton btnAbrir = new JButton("Abrir documento SVG");
        btnAbrir.addActionListener(e -> cargarArchivoSVG()); add(btnAbrir, BorderLayout.NORTH);
    }

    private void cargarArchivoSVG() {
        JFileChooser fc = new JFileChooser(new File("."));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            List<FiguraSVG> figurasProcesadas = new ArrayList<>();
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true); factory.setExpandEntityReferences(false);
                Document doc = factory.newDocumentBuilder().parse(fc.getSelectedFile());
                doc.getDocumentElement().normalize();
                procesarNodos(doc.getDocumentElement().getChildNodes(), figurasProcesadas);
                lienzo.setFiguras(figurasProcesadas);
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); }
        }
    }

    private void procesarNodos(NodeList nodos, List<FiguraSVG> figuras) {
        for (int i = 0; i < nodos.getLength(); i++) {
            Node nodo = nodos.item(i);
            if (nodo.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) nodo; String tag = el.getNodeName(); FiguraSVG fig = null;
                try {
                    switch (tag) {
                        case "line": fig = new Linea(getDbl(el,"x1"), getDbl(el,"y1"), getDbl(el,"x2"), getDbl(el,"y2")); break;
                        case "circle": fig = new Circulo(getDbl(el,"cx"), getDbl(el,"cy"), getDbl(el,"r")); break;
                        case "rect": fig = new Rectangulo(getDbl(el,"x"), getDbl(el,"y"), getDbl(el,"width"), getDbl(el,"height")); break;
                        case "ellipse": fig = new Elipse(getDbl(el,"cx"), getDbl(el,"cy"), getDbl(el,"rx"), getDbl(el,"ry")); break;
                        case "polyline": fig = new Polilinea(el.getAttribute("points")); break;
                        case "text": fig = new Texto(getDbl(el,"x"), getDbl(el,"y"), el.getTextContent(), el.getAttribute("font-family"), (int)getDbl(el,"font-size")); break;
                        case "g": procesarNodos(el.getChildNodes(), figuras); break;
                    }
                    if (fig != null) {
                        String str = el.getAttribute("stroke"), fil = el.getAttribute("fill"), sw = el.getAttribute("stroke-width");
                        if (!str.isEmpty()) fig.stroke = parseColor(str);
                        if (!fil.isEmpty() && !fil.equals("none")) fig.fill = parseColor(fil);
                        if (!sw.isEmpty()) fig.strokeWidth = (float) getDbl(el, "stroke-width");
                        figuras.add(fig);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private double getDbl(Element e, String attr) {
        String val = e.getAttribute(attr); return val.isEmpty() ? 0 : Double.parseDouble(val.replaceAll("[a-zA-Z]", ""));
    }
    private Color parseColor(String c) {
        if (c.startsWith("#")) return Color.decode(c);
        switch (c.toLowerCase()) {
            case "red": return Color.RED; case "blue": return Color.BLUE; case "green": return Color.GREEN;
            case "yellow": return Color.YELLOW; case "black": return Color.BLACK; case "orange": return Color.ORANGE;
            case "lightblue": return new Color(173, 216, 230); case "navy": return new Color(0, 0, 128);
            case "lightgreen": return new Color(144, 238, 144); default: return Color.BLACK;
        }
    }
    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new VisorSVG().setVisible(true)); }
}