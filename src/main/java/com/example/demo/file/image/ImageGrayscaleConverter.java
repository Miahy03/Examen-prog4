package com.example.demo.file.image;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
public class ImageGrayscaleConverter {

  @SneakyThrows
  public File toGrayscale(File source) {
    var originalImage = ImageIO.read(source);
    var grayscaleImage =
        new BufferedImage(
            originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

    var graphics = grayscaleImage.getGraphics();
    graphics.drawImage(originalImage, 0, 0, null);
    graphics.dispose();

    var grayscaleFile = File.createTempFile("grayscale-", ".jpg");
    ImageIO.write(grayscaleImage, "jpg", grayscaleFile);
    return grayscaleFile;
  }
}
