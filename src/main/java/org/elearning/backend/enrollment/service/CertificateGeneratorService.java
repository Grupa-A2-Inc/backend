package org.elearning.backend.enrollment.service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.Image;

public class CertificateGeneratorService {


    private BaseFont titleFont;
    private BaseFont certificationFont;
    private BaseFont signatureFont;
    private BaseFont applicationNameFont;


    private static final float HEIGHT = PageSize.A4.getHeight();
    private static final float WIDTH = PageSize.A4.getWidth();

    //RECTANGLE CONSTANTS
    private static final float RECTANGLE_X_ORIGIN = WIDTH/14f;
    private static final float RECTANGLE_Y_ORIGIN = HEIGHT/20f;
    private static final float RECTANGLE_X_END = 12*WIDTH/14;
    private static final float RECTANGLE_Y_END = 18*HEIGHT/20;

    //CORNER CONSTANTS

    private static final float CORNER_LEFT_MARGIN  = WIDTH / 12f;
    private static final float CORNER_RIGHT_MARGIN = 11 * WIDTH / 12f;

    private static final float CORNER_TOP_MARGIN    = 17 * HEIGHT / 18f;
    private static final float CORNER_BOTTOM_MARGIN = HEIGHT / 18f;

    private static final float CORNER_TOP_INNER    = 15 * HEIGHT / 18f;
    private static final float CORNER_BOTTOM_INNER = 3 * HEIGHT / 18f;
    private static final float CORNER_LENGTH_SIZE = WIDTH/6f;

    //BOTTOM LAYOUT CONSTANTS

    private static final float BOTTOM_HEIGHT_UNIT = HEIGHT / 36;
    private static final float BOTTOM_WIDTH_UNIT = WIDTH / 48;
    private static final float BOTTOM_Y_LINE_COORDINATE = 8 * BOTTOM_HEIGHT_UNIT;
    private static final float BOTTOM_LINE_LENGTH = BOTTOM_WIDTH_UNIT * 10;
    private static final float BOTTOM_Y_PLACED_TEXT_COORDINATE = BOTTOM_Y_LINE_COORDINATE + BOTTOM_HEIGHT_UNIT/2;
    private static final float BOTTOM_Y_CUSTOM_TEXT_COORDINATE = BOTTOM_Y_LINE_COORDINATE - BOTTOM_HEIGHT_UNIT;
    private static final String SERIAL_ID_FIELD = "Serial ID";
    private static final String DATE_FIELD = "Date";

    //GENERAL CERTIFICATE CONTENT CONSTANTS

    private static final float CONTENT_HEIGHT_UNIT = HEIGHT/60;
    private static final float CONTENT_X_CENTERED_IMAGE_POSITION = 7*WIDTH/17;
    private static final float CONTENT_IMAGE_WIDTH = 150;
    private static final float CONTENT_IMAGE_HEIGHT = 150;
    private static final float CONTENT_X_CENTERED_TEXT_POSITION = WIDTH/2;
    private static final float CONTENT_SIGNATURE_X_ORIGIN = 2*WIDTH/8;
    private static final float CONTENT_SIGNATURE_X_SIZE = 4*WIDTH/8;

    private static final float TITLE_FONT_SIZE = 36;
    private static final float APPLICATION_NAME_FONT_SIZE = 24;
    private static final float CERTIFICATION_FONT_SIZE = 16;
    private static final float SIGNATURE_FONT_SIZE = 48;


    private static final String PATH_SITE_LOGO = "/images/crap_logo.png";
    private static final String CERTIFICATE_OF = "Certificate of";
    private static final String COMPLETION = "Completion";
    private static final String APPLICATION_NAME = "E-Learning";
    private static final String CERTIFYING  = "That is to certify that";
    private static final String SUCCESS_MESSAGE = "Has successfully completed";

    //FONT PATH
    private static final String PATH_QUADRILLION_FONT = "/fonts/Quadrillion-Sb.otf";
    private static final String PATH_JASTYKA_FONT = "/fonts/Jastyka.ttf";


    //DOCUMENT COLOR SCHEME
    private static final Color DOCUMENT_COLOR_SCHEME = new Color(23, 70, 124);



    private void loadFont() throws IOException {

        try (
                InputStream quadrillionFont = getClass().getResourceAsStream(PATH_QUADRILLION_FONT);
                InputStream jastykaFont = getClass().getResourceAsStream(PATH_JASTYKA_FONT)
        ) {
            if (quadrillionFont == null) {
                throw new IllegalArgumentException("Font not found: " + PATH_QUADRILLION_FONT);
            }
            if (jastykaFont == null) {
                throw new IllegalArgumentException("Font not found: " + PATH_JASTYKA_FONT);
            }

            byte[] quadrillionBytes = quadrillionFont.readAllBytes();
            byte[] jastykaBytes = jastykaFont.readAllBytes();

            if (titleFont == null) {
                titleFont = BaseFont.createFont(PATH_QUADRILLION_FONT, BaseFont.WINANSI, BaseFont.EMBEDDED, false, quadrillionBytes, null);
            }
            if (certificationFont == null) {
                certificationFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
            }
            if (signatureFont == null) {
                signatureFont = BaseFont.createFont(PATH_JASTYKA_FONT, BaseFont.WINANSI, BaseFont.EMBEDDED, false, jastykaBytes, null);
            }
            if (applicationNameFont == null) {
                applicationNameFont = BaseFont.createFont(PATH_QUADRILLION_FONT, BaseFont.WINANSI, BaseFont.EMBEDDED, false, quadrillionBytes, null);
            }
        }
    }

    private void drawCorner(PdfContentByte canvas, float xCoordinate, float yCoordinate, float xLength, float yLength){
        canvas.moveTo(xCoordinate, yCoordinate);   // colt 1
        canvas.lineTo(xLength, yCoordinate);  // colt 2
        canvas.lineTo(xCoordinate, yLength); // colt 3
        canvas.closePath();
        canvas.setColorFill(DOCUMENT_COLOR_SCHEME);
        canvas.fill();
        canvas.stroke();
    }

    private void generateLayout(PdfContentByte canvas){
        canvas.setColorStroke(DOCUMENT_COLOR_SCHEME);
        canvas.rectangle(RECTANGLE_X_ORIGIN, RECTANGLE_Y_ORIGIN, RECTANGLE_X_END, RECTANGLE_Y_END);
        canvas.stroke();


        drawCorner(canvas, CORNER_LEFT_MARGIN, CORNER_TOP_MARGIN, CORNER_LEFT_MARGIN + CORNER_LENGTH_SIZE, CORNER_TOP_INNER);
        drawCorner(canvas, CORNER_RIGHT_MARGIN, CORNER_TOP_MARGIN, CORNER_RIGHT_MARGIN - CORNER_LENGTH_SIZE, CORNER_TOP_INNER);

        drawCorner(canvas, CORNER_LEFT_MARGIN, CORNER_BOTTOM_MARGIN, CORNER_LEFT_MARGIN + CORNER_LENGTH_SIZE, CORNER_BOTTOM_INNER);
        drawCorner(canvas, CORNER_RIGHT_MARGIN, CORNER_BOTTOM_MARGIN, CORNER_RIGHT_MARGIN - CORNER_LENGTH_SIZE, CORNER_BOTTOM_INNER);

    }

    private void generateBottomField(PdfContentByte canvas, String fixedField, String customField, float currentLinePosition){
        canvas.moveTo(currentLinePosition, BOTTOM_Y_LINE_COORDINATE);
        canvas.lineTo(currentLinePosition + BOTTOM_LINE_LENGTH, BOTTOM_Y_LINE_COORDINATE);

        canvas.beginText();
        canvas.setFontAndSize(certificationFont, 12);
        canvas.showTextAligned(
                PdfContentByte.ALIGN_CENTER,
                fixedField,    // text content
                currentLinePosition + BOTTOM_LINE_LENGTH / 2,                          // x position
                BOTTOM_Y_PLACED_TEXT_COORDINATE,                          // y position
                0                             // degree rotation
        );

        canvas.showTextAligned(
                PdfContentByte.ALIGN_CENTER,
                customField,    // text content
                currentLinePosition + BOTTOM_LINE_LENGTH / 2,                          // X position
                BOTTOM_Y_CUSTOM_TEXT_COORDINATE,                          // Y position
                0                             // degree rotation
        );
        canvas.endText();
    }

    private void generateBottomLayout(PdfContentByte canvas, UUID enrollmentId, LocalDate completedAt){
        canvas.setColorStroke(DOCUMENT_COLOR_SCHEME);
        float currentLineStartPosition = BOTTOM_WIDTH_UNIT*7;
        generateBottomField(canvas, SERIAL_ID_FIELD, enrollmentId.toString().substring(0, 8), currentLineStartPosition);

        currentLineStartPosition += 2 * BOTTOM_LINE_LENGTH + 4 * BOTTOM_WIDTH_UNIT;

        generateBottomField(canvas, DATE_FIELD, completedAt.toString(), currentLineStartPosition);

        canvas.stroke();
    }

    private void generateImage(PdfContentByte canvas, float yPosition) throws IOException {
        try{
            byte[] imageBytes = Objects.requireNonNull(CertificateGeneratorService.class.getResourceAsStream(PATH_SITE_LOGO)).readAllBytes();
            Image icon = Image.getInstance(imageBytes);
            icon.setAbsolutePosition(CONTENT_X_CENTERED_IMAGE_POSITION, yPosition);
            icon.scaleToFit(CONTENT_IMAGE_WIDTH, CONTENT_IMAGE_HEIGHT);
            canvas.addImage(icon);
        }
        catch (IOException exception){
            throw new IOException("Image could not be loaded", exception);
        }
    }

    private void generateTitle(PdfContentByte canvas, float yPosition){
        canvas.setColorFill(Color.BLACK);
        canvas.beginText();

        canvas.setFontAndSize(titleFont, TITLE_FONT_SIZE);
        canvas.showTextAligned(
                PdfContentByte.ALIGN_CENTER,
                CERTIFICATE_OF,
                CONTENT_X_CENTERED_TEXT_POSITION,
                yPosition,
                0
        );

        yPosition -= 2*CONTENT_HEIGHT_UNIT;

        canvas.showTextAligned(
                PdfContentByte.ALIGN_CENTER,
                COMPLETION,
                CONTENT_X_CENTERED_TEXT_POSITION,
                yPosition,
                0
        );
        canvas.endText();
    }

    private void generateApplicationName(PdfContentByte canvas, float yPosition){
        canvas.beginText();

        canvas.setFontAndSize(applicationNameFont, APPLICATION_NAME_FONT_SIZE);
        canvas.showTextAligned(
                PdfContentByte.ALIGN_CENTER,
                APPLICATION_NAME,
                CONTENT_X_CENTERED_TEXT_POSITION,
                yPosition,
                0
        );
        canvas.endText();
    }

    private void generateCertificationMessage(PdfContentByte canvas, float yPosition, String textContent){
        canvas.beginText();
        canvas.setFontAndSize(certificationFont, CERTIFICATION_FONT_SIZE);

        canvas.showTextAligned(
                PdfContentByte.ALIGN_CENTER,
                textContent,
                CONTENT_X_CENTERED_TEXT_POSITION,
                yPosition,
                0
        );
        canvas.endText();
    }

    private void insertName(PdfContentByte canvas, float yPosition, String studentName){
        canvas.setColorStroke(DOCUMENT_COLOR_SCHEME);
        canvas.beginText();
        canvas.setFontAndSize(signatureFont, SIGNATURE_FONT_SIZE);
        canvas.showTextAligned(
                PdfContentByte.ALIGN_CENTER,
                studentName,
                CONTENT_X_CENTERED_TEXT_POSITION,
                yPosition,
                0
        );
        canvas.endText();

        canvas.moveTo(CONTENT_SIGNATURE_X_ORIGIN, yPosition);
        canvas.lineTo(CONTENT_SIGNATURE_X_ORIGIN + CONTENT_SIGNATURE_X_SIZE, yPosition);
        canvas.stroke();
    }

    private void generateContent(PdfContentByte canvas, String studentName, String courseTitle) throws IOException {
        float currentPosition = 45 * CONTENT_HEIGHT_UNIT;

        generateImage(canvas, currentPosition);
        currentPosition -= 3*CONTENT_HEIGHT_UNIT/2;
        generateTitle(canvas, currentPosition);
        currentPosition -= 4*CONTENT_HEIGHT_UNIT;
        generateApplicationName(canvas, currentPosition);
        currentPosition -= 3*CONTENT_HEIGHT_UNIT;
        generateCertificationMessage(canvas, currentPosition, CERTIFYING);
        currentPosition -= 5*CONTENT_HEIGHT_UNIT;
        insertName(canvas, currentPosition, studentName);
        currentPosition -= 3*CONTENT_HEIGHT_UNIT;
        generateCertificationMessage(canvas, currentPosition, SUCCESS_MESSAGE);
        currentPosition -= 3*CONTENT_HEIGHT_UNIT;
        generateCertificationMessage(canvas, currentPosition, courseTitle);

    }





    public CertificateGeneratorService() throws IOException {
        loadFont();
    }

    public byte[] generatePdf(UUID enrollmentId, String studentName, String courseTitle, LocalDate completedAt){
        Document document = new Document(PageSize.A4);
        try(ByteArrayOutputStream generatedPdfBytes = new ByteArrayOutputStream()) {

            final PdfWriter instance = PdfWriter.getInstance(document, generatedPdfBytes);
            document.open();
            PdfContentByte canvas = instance.getDirectContent();
            generateLayout(canvas);
            generateBottomLayout(canvas, enrollmentId, completedAt);
            generateContent(canvas, studentName, courseTitle);
            document.close();
            return generatedPdfBytes.toByteArray();

        } catch (DocumentException | IOException documentException) {
            throw new RuntimeException("PDF generation failed", documentException);
        }
    }
}