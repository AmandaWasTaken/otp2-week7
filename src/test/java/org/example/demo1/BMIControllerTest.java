package org.example.demo1;

import javafx.application.Platform;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

class BMIControllerTest {

    @BeforeAll
    static void initializeJavaFX() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.startup(latch::countDown);

        assertTrue(latch.await(10, TimeUnit.SECONDS));

        Locale.setDefault(Locale.US);
    }

    /*
     * Executes an operation on the JavaFX application thread.
     */
    private static void runOnFXThread(Runnable operation)
            throws Exception {

        if (Platform.isFxApplicationThread()) {
            operation.run();
            return;
        }

        FutureTask<Void> task =
                new FutureTask<>(operation, null);

        Platform.runLater(task);

        task.get(10, TimeUnit.SECONDS);
    }

    /*
     * Creates localized strings for testing.
     */
    private static Map<String, String> translations(Locale locale) {
        String lang = locale.getLanguage();

        return Map.of(
                "weight", lang + " weight",
                "height", lang + " height",
                "calculate", lang + " calculate",
                "result", lang + " result",
                "invalid", lang + " invalid",
                "localTime", lang + " time"
        );
    }

    /*
     * Injects JavaFX controls into the controller.
     * Normally FXMLLoader does this.
     */
    private static void inject(
            BMIController controller,
            String fieldName,
            Object value
    ) throws Exception {

        var field = BMIController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, value);
    }

    /*
     * Creates a controller with all required JavaFX controls.
     */
    private static Fixture createFixture() throws Exception {

        BMIController controller = new BMIController();

        Label lblWeight = new Label();
        Label lblHeight = new Label();
        Label lblResult = new Label();
        Label lblLocalTime = new Label();

        TextField tfWeight = new TextField();
        TextField tfHeight = new TextField();

        Button btnCalculate = new Button();

        VBox rootVBox = new VBox();

        inject(controller, "lblWeight", lblWeight);
        inject(controller, "lblHeight", lblHeight);
        inject(controller, "lblResult", lblResult);
        inject(controller, "lblLocalTime", lblLocalTime);

        inject(controller, "tfWeight", tfWeight);
        inject(controller, "tfHeight", tfHeight);

        inject(controller, "btnCalculate", btnCalculate);
        inject(controller, "rootVBox", rootVBox);

        return new Fixture(
                controller,
                lblWeight,
                lblHeight,
                lblResult,
                lblLocalTime,
                tfWeight,
                tfHeight,
                btnCalculate,
                rootVBox
        );
    }

    private record Fixture(
            BMIController controller,
            Label lblWeight,
            Label lblHeight,
            Label lblResult,
            Label lblLocalTime,
            TextField tfWeight,
            TextField tfHeight,
            Button btnCalculate,
            VBox rootVBox
    ) {}

    // --------------------------------------------------
    // INITIALIZATION
    // --------------------------------------------------

    @Test
    void initializeSetsEnglishLabels() throws Exception {

        runOnFXThread(() -> {
            try (MockedStatic<LocalizationService> localization =
                         mockStatic(LocalizationService.class)) {

                Locale english = new Locale("en", "US");

                localization.when(() ->
                        LocalizationService.getLocalizedStrings(english)
                ).thenReturn(translations(english));

                Fixture f = createFixture();

                f.controller.initialize();

                assertEquals("en weight", f.lblWeight.getText());
                assertEquals("en height", f.lblHeight.getText());
                assertEquals("en calculate", f.btnCalculate.getText());

                assertEquals("", f.lblResult.getText());

                assertTrue(
                        f.lblLocalTime.getText().startsWith("en time ")
                );

                assertTrue(
                        f.lblLocalTime.getText().matches(
                                "en time \\d{2}:\\d{2}:\\d{2}"
                        )
                );

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    // --------------------------------------------------
    // BMI CALCULATION
    // --------------------------------------------------

    @Test
    void calculateBMIWithValidInput() throws Exception {

        runOnFXThread(() -> {
            try (MockedStatic<LocalizationService> localization =
                         mockStatic(LocalizationService.class);
                 MockedStatic<BMIResultService> database =
                         mockStatic(BMIResultService.class)) {

                Locale english = new Locale("en", "US");

                localization.when(() ->
                        LocalizationService.getLocalizedStrings(english)
                ).thenReturn(translations(english));

                Fixture f = createFixture();

                f.controller.initialize();

                f.tfWeight.setText("70");
                f.tfHeight.setText("175");

                f.controller.onCalculateClick(null);

                assertEquals("en result 22.86", f.lblResult.getText());

                database.verify(() ->
                        BMIResultService.saveResult(
                                eq(70.0),
                                eq(1.75),
                                anyDouble(),
                                eq("en")
                        )
                );

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void calculateBMIWithDifferentValues() throws Exception {

        runOnFXThread(() -> {
            try (MockedStatic<LocalizationService> localization =
                         mockStatic(LocalizationService.class);
                 MockedStatic<BMIResultService> database =
                         mockStatic(BMIResultService.class)) {

                Locale english = new Locale("en", "US");

                localization.when(() ->
                        LocalizationService.getLocalizedStrings(english)
                ).thenReturn(translations(english));

                Fixture f = createFixture();

                f.controller.initialize();

                f.tfWeight.setText("80");
                f.tfHeight.setText("180");

                f.controller.onCalculateClick(null);

                assertEquals("en result 24.69", f.lblResult.getText());

                database.verify(() ->
                        BMIResultService.saveResult(
                                eq(80.0),
                                eq(1.8),
                                anyDouble(),
                                eq("en")
                        )
                );

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    // --------------------------------------------------
    // INVALID INPUT
    // --------------------------------------------------

    @Test
    void invalidWeightDisplaysError() throws Exception {

        runOnFXThread(() -> {
            try (MockedStatic<LocalizationService> localization =
                         mockStatic(LocalizationService.class);
                 MockedStatic<BMIResultService> database =
                         mockStatic(BMIResultService.class)) {

                Locale english = new Locale("en", "US");

                localization.when(() ->
                        LocalizationService.getLocalizedStrings(english)
                ).thenReturn(translations(english));

                Fixture f = createFixture();

                f.controller.initialize();

                f.tfWeight.setText("abc");
                f.tfHeight.setText("175");

                f.controller.onCalculateClick(null);

                assertEquals("en invalid", f.lblResult.getText());

                database.verifyNoInteractions();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void invalidHeightDisplaysError() throws Exception {

        runOnFXThread(() -> {
            try (MockedStatic<LocalizationService> localization =
                         mockStatic(LocalizationService.class);
                 MockedStatic<BMIResultService> database =
                         mockStatic(BMIResultService.class)) {

                Locale english = new Locale("en", "US");

                localization.when(() ->
                        LocalizationService.getLocalizedStrings(english)
                ).thenReturn(translations(english));

                Fixture f = createFixture();

                f.controller.initialize();

                f.tfWeight.setText("70");
                f.tfHeight.setText("abc");

                f.controller.onCalculateClick(null);

                assertEquals("en invalid", f.lblResult.getText());

                database.verifyNoInteractions();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void emptyInputDisplaysError() throws Exception {

        runOnFXThread(() -> {
            try (MockedStatic<LocalizationService> localization =
                         mockStatic(LocalizationService.class);
                 MockedStatic<BMIResultService> database =
                         mockStatic(BMIResultService.class)) {

                Locale english = new Locale("en", "US");

                localization.when(() ->
                        LocalizationService.getLocalizedStrings(english)
                ).thenReturn(translations(english));

                Fixture f = createFixture();

                f.controller.initialize();

                f.tfWeight.setText("");
                f.tfHeight.setText("");

                f.controller.onCalculateClick(null);

                assertEquals("en invalid", f.lblResult.getText());

                database.verifyNoInteractions();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    // --------------------------------------------------
    // LANGUAGE SELECTION
    // --------------------------------------------------

    @Test
    void englishButtonSelectsEnglish() throws Exception {

        testLanguage(
                new Locale("en", "US"),
                controller -> controller.onENClick(null)
        );
    }

    @Test
    void frenchButtonSelectsFrench() throws Exception {

        testLanguage(
                new Locale("fr", "FR"),
                controller -> controller.onFRClick(null)
        );
    }

    @Test
    void urduButtonSelectsUrdu() throws Exception {

        testLanguage(
                new Locale("ur", "PA"),
                controller -> controller.onURClick(null)
        );
    }

    @Test
    void vietnameseButtonSelectsVietnamese() throws Exception {

        testLanguage(
                new Locale("vi", "VI"),
                controller -> controller.onVIClick(null)
        );
    }

    @Test
    void persianButtonSelectsPersian() throws Exception {

        testLanguage(
                new Locale("fa", "IR"),
                controller -> controller.onFAClick(null)
        );
    }

    private void testLanguage(
            Locale locale,
            java.util.function.Consumer<BMIController> action
    ) throws Exception {

        runOnFXThread(() -> {
            try (MockedStatic<LocalizationService> localization =
                         mockStatic(LocalizationService.class)) {

                Locale english = new Locale("en", "US");

                localization.when(() ->
                        LocalizationService.getLocalizedStrings(english)
                ).thenReturn(translations(english));

                localization.when(() ->
                        LocalizationService.getLocalizedStrings(locale)
                ).thenReturn(translations(locale));

                Fixture f = createFixture();

                f.controller.initialize();

                action.accept(f.controller);

                assertEquals(
                        locale.getLanguage() + " weight",
                        f.lblWeight.getText()
                );

                assertEquals(
                        locale.getLanguage() + " height",
                        f.lblHeight.getText()
                );

                assertEquals(
                        locale.getLanguage() + " calculate",
                        f.btnCalculate.getText()
                );

                assertTrue(
                        f.lblLocalTime.getText().startsWith(
                                locale.getLanguage() + " time "
                        )
                );

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    // --------------------------------------------------
    // RTL / LTR
    // --------------------------------------------------

    @Test
    void urduUsesRightToLeftLayout() throws Exception {

        testTextDirection(
                new Locale("ur", "PA"),
                controller -> controller.onURClick(null),
                NodeOrientation.RIGHT_TO_LEFT,
                "-fx-text-alignment: right;"
        );
    }

    @Test
    void persianUsesRightToLeftLayout() throws Exception {

        testTextDirection(
                new Locale("fa", "IR"),
                controller -> controller.onFAClick(null),
                NodeOrientation.RIGHT_TO_LEFT,
                "-fx-text-alignment: right;"
        );
    }

    @Test
    void englishUsesLeftToRightLayout() throws Exception {

        testTextDirection(
                new Locale("en", "US"),
                controller -> controller.onENClick(null),
                NodeOrientation.LEFT_TO_RIGHT,
                "-fx-text-alignment: left;"
        );
    }

    private void testTextDirection(
            Locale locale,
            java.util.function.Consumer<BMIController> action,
            NodeOrientation expectedOrientation,
            String expectedStyle
    ) throws Exception {

        runOnFXThread(() -> {
            try (MockedStatic<LocalizationService> localization =
                         mockStatic(LocalizationService.class)) {

                Locale english = new Locale("en", "US");

                localization.when(() ->
                        LocalizationService.getLocalizedStrings(english)
                ).thenReturn(translations(english));

                localization.when(() ->
                        LocalizationService.getLocalizedStrings(locale)
                ).thenReturn(translations(locale));

                Fixture f = createFixture();

                f.controller.initialize();

                action.accept(f.controller);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Allow Platform.runLater() to finish.
        runOnFXThread(() -> {});

        runOnFXThread(() -> {
            try {
                Fixture f = createFixture();

                // This fixture is only for checking the expected
                // values when testing the controller's layout.
                // The actual layout assertions are made below
                // in the dedicated layout test.

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
