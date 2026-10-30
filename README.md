\# Visualizador SVG con Java DOM y Java 2D



\## Arquitectura y Decisiones

El proyecto utiliza una jerarquía de clases polimórfica donde una clase abstracta `FiguraSVG` define el método `dibujar(Graphics2D)`. Las seis subclases (Linea, Circulo, Rectangulo, Elipse, Polilinea, Texto) implementan este método usando primitivas de Java 2D. El método `paintComponent` del JPanel itera la lista de figuras delegando el dibujado. Se separó la lógica de lectura XML (DocumentBuilder) del dibujado para no afectar el rendimiento al repintar la interfaz.



\## Limitaciones

El renderizado se limita a colores básicos/hexadecimales y no soporta transformaciones, viewBox ni atributos definidos por CSS externo.



\## Preguntas de Reflexión

1\. DOM carga todo el árbol jerárquico en memoria permitiendo un acceso aleatorio y conservando fácilmente el orden (Z-index), mientras SAX lee de forma secuencial. La desventaja de DOM es su mayor consumo de memoria.

2\. Cada nodo elemento XML (`<circle>`) se mapea a la instanciación de un objeto en Java (`new Circulo()`), y los atributos XML (`cx`, `cy`) se mapean a las propiedades internas de dicho objeto.

3\. El polimorfismo permite que el motor de dibujo (`paintComponent`) recorra la lista de objetos invocando `.dibujar()` sin usar estructuras `switch` ni saber qué figura específica está procesando.

4\. Separar la lógica evita re-procesar el archivo XML cada vez que la ventana requiere ser repintada, mejorando drásticamente el rendimiento de la UI.

5\. Implica lidiar con conversiones de tipos (String a Double), manejar errores de formato (`NumberFormatException`), limpiar espacios en blanco (ej. atributo points) y crear lógica propia para procesar colores hexadecimales.

6\. Se requeriría crear una nueva subclase que herede de `FiguraSVG`, implementar su método `dibujar()` con las clases de Java 2D correspondientes, y agregar un `case` al bloque `switch` del parseo.

7\. Requeriría parsear el atributo `transform`, interpretar los valores (ej. `translate(x,y)`) y aplicar los métodos equivalentes de la clase `AffineTransform` al objeto `Graphics2D` justo antes de trazar la figura.



