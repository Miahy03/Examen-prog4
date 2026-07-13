package com.example.demo.file.image;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ImageGrayscaleConverterTest {

  private final ImageGrayscaleConverter converter = new ImageGrayscaleConverter();

  @Test
  void toGrayscale_convertsColoredImageToGrayscale() throws Exception {
    var coloredImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
    var graphics = coloredImage.getGraphics();
    graphics.setColor(Color.RED);
    graphics.fillRect(0, 0, 10, 10);
    graphics.dispose();
    var sourceFile = File.createTempFile("colored-", ".jpg");
    ImageIO.write(coloredImage, "jpg", sourceFile);

    var grayscaleFile = converter.toGrayscale(sourceFile);

    assertThat(grayscaleFile).exists();
    var grayscaleImage = ImageIO.read(grayscaleFile);
    var pixel = new Color(grayscaleImage.getRGB(5, 5));
    assertThat(pixel.getRed()).isEqualTo(pixel.getGreen());
    assertThat(pixel.getGreen()).isEqualTo(pixel.getBlue());
  }
}
