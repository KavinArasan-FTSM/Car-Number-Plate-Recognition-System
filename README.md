# ANPR System (Automatic Number Plate Recognition)

**Built with:** Java, Swing, BufferedImage  
**IDE:** Eclipse  

---

## Description
This project implements a basic **Automatic Number Plate Recognition (ANPR)** system using image processing techniques.  
It detects and highlights the license plate region from input car images using the following steps:

1. Convert image to **grayscale**  
2. Apply **median filtering** to reduce noise  
3. Apply **Sobel edge detection** to highlight vertical edges  
4. Compute **vertical projection** to locate candidate bands for license plates  
5. Crop and display candidate bands for verification  

A simple GUI is provided to **upload images and display results**.

---

## Demo

| Step | Description |
|------|-------------|
| Original | Input car image |
| Grayscale | Converted grayscale image |
| Median Filter | Noise reduction using median filter |
| Vertical Edge | Sobel vertical edge detection applied |
| Candidate Band | Potential license plate regions cropped |
| Final Output | Final selected license plate band |

---

## How to Run

1. Clone the repository:

```bash
git clone https://github.com/<yourusername>/KavinArasan-FTSM.git

2. Open the project in Eclipse IDE.

3. Navigate to src/ and open ANPR.java.

4. Run ANPR.java.

5. Click the Upload Image button and select an image of a car.

6. The program will display:

   Original image

   Grayscale image

   Filtered image

   Edge-detected image

   Candidate license plate bands

   Final selected band

