package com.example.genoclasifica.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Flujo web del portal con el navegador real (sección 7.5). Levanta la app completa en un puerto libre.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PortalSeleniumTest {

    private static final Path DESCARGAS = Path.of("target", "selenium-descargas").toAbsolutePath();
    private static final Path CAPTURAS = Path.of("target", "selenium-capturas");

    @LocalServerPort
    private int puerto;

    private WebDriver driver;
    private WebDriverWait espera;
    private final String sufijo = String.valueOf(System.currentTimeMillis());

    @BeforeEach
    void abrirNavegador() throws Exception {
        Files.createDirectories(DESCARGAS);
        ChromeOptions opciones = new ChromeOptions();
        opciones.addArguments("--headless=new", "--window-size=1280,1000");
        opciones.setExperimentalOption("prefs", Map.of("download.default_directory", DESCARGAS.toString()));
        driver = new ChromeDriver(opciones);
        espera = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get("http://localhost:" + puerto + "/");
    }

    @AfterEach
    void cerrarNavegador(TestInfo test) throws Exception {
        Files.createDirectories(CAPTURAS);
        File captura = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        Files.copy(captura.toPath(), CAPTURAS.resolve(test.getTestMethod().orElseThrow().getName() + ".png"),
            StandardCopyOption.REPLACE_EXISTING);
        driver.quit();
    }

    private void ingresar(String usuario) {
        if (driver.findElement(By.id("btn-salir")).isDisplayed()) {
            driver.findElement(By.id("btn-salir")).click();
        }
        escribir("usuario", usuario);
        escribir("clave", "Clave-Sintetica-123");
        driver.findElement(By.id("btn-ingresar")).click();
        espera.until(ExpectedConditions.textToBe(By.id("usuario-actual"), usuario));
    }

    private void escribir(String id, String texto) {
        WebElement campo = driver.findElement(By.id(id));
        campo.clear();
        campo.sendKeys(texto);
    }

    private void registrar(String codigo, String contaminacion, String categoria) {
        escribir("reg-codigo", codigo);
        escribir("reg-corrida", "RUN-" + codigo);
        escribir("reg-contenido", "corrida sintetica " + codigo);
        escribir("reg-q30", "92");
        escribir("reg-contaminacion", contaminacion);
        new Select(driver.findElement(By.id("reg-categoria"))).selectByVisibleText(categoria);
        driver.findElement(By.id("btn-registrar")).click();
        esperarMensaje("registrada");
    }

    private void abrir(String codigo) {
        escribir("buscar-codigo", codigo);
        driver.findElement(By.id("btn-buscar")).click();
        espera.until(ExpectedConditions.textToBe(By.id("det-codigo"), codigo));
    }

    private void esperarMensaje(String texto) {
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("mensaje"), texto));
    }

    private void esperarEstado(String estado) {
        espera.until(ExpectedConditions.textToBe(By.id("det-estado"), estado));
    }

    private boolean visible(String id) {
        return driver.findElement(By.id(id)).isDisplayed();
    }

    @Test
    void claveIncorrectaMuestraErrorYNoEntra() {
        escribir("usuario", "genetista_1");
        escribir("clave", "incorrecta");
        driver.findElement(By.id("btn-ingresar")).click();

        esperarMensaje("Usuario o clave incorrectos");
        assertThat(visible("pantalla-principal")).isFalse();
    }

    @Test
    void cadaRolVeSoloLasAccionesQuePuedeHacer() {
        String codigo = "WEB-" + sufijo + "-R";
        ingresar("tecnico_lab");
        assertThat(visible("registro")).isTrue();
        registrar(codigo, "1", "BENIGNA");
        assertThat(visible("btn-informe")).isTrue();
        assertThat(visible("btn-procesar")).isFalse();
        assertThat(visible("btn-firmar")).isFalse();

        ingresar("bio_user");
        abrir(codigo);
        assertThat(visible("registro")).isFalse();
        assertThat(visible("btn-procesar")).isTrue();
        assertThat(visible("btn-firmar")).isFalse();
        assertThat(visible("btn-informe")).isFalse();

        ingresar("genetista_1");
        abrir(codigo);
        assertThat(visible("btn-firmar")).isTrue();
        assertThat(visible("btn-procesar")).isFalse();

        ingresar("auditor");
        abrir(codigo);
        assertThat(visible("registro")).isFalse();
        assertThat(visible("btn-procesar")).isFalse();
        assertThat(visible("btn-firmar")).isFalse();
        assertThat(visible("btn-informe")).isFalse();
    }

    @Test
    void variantePatogenicaRequiereFirmaAntesDelInforme() throws Exception { // T1, T7, T8, TI1, T10, T11
        String codigo = "WEB-" + sufijo + "-P";
        ingresar("tecnico_lab");
        registrar(codigo, "1", "PATOGENICA");
        esperarEstado("Recibida");

        ingresar("bio_user");
        abrir(codigo);
        driver.findElement(By.id("btn-procesar")).click();
        esperarEstado("Pendiente de firma");

        ingresar("tecnico_lab");
        abrir(codigo);
        driver.findElement(By.id("btn-informe")).click();
        esperarMensaje("Requisito de firma no cumplido");

        ingresar("genetista_1");
        abrir(codigo);
        driver.findElement(By.id("btn-firmar")).click();
        esperarEstado("Firmada");
        assertThat(driver.findElement(By.id("det-firma")).getText()).isEqualTo("genetista_1");

        driver.findElement(By.id("btn-informe")).click();
        esperarMensaje("descargado");
        esperarEstado("Informe emitido");
        Path pdf = DESCARGAS.resolve("informe-" + codigo + ".pdf");
        espera.until(d -> Files.exists(pdf));
        assertThat(Files.readString(pdf.toAbsolutePath(), java.nio.charset.StandardCharsets.ISO_8859_1)).startsWith("%PDF-");
    }

    @Test
    void muestraContaminadaQuedaBloqueadaYNoEmiteInforme() { // T5, TI3 · CP-ST-INV-03
        String codigo = "WEB-" + sufijo + "-B";
        ingresar("tecnico_lab");
        registrar(codigo, "3.5", "PATOGENICA");

        ingresar("bio_user");
        abrir(codigo);
        driver.findElement(By.id("btn-procesar")).click();
        esperarEstado("Bloqueada");

        ingresar("genetista_1");
        abrir(codigo);
        driver.findElement(By.id("btn-informe")).click();
        esperarMensaje("bloqueada");
        assertThat(driver.findElement(By.id("det-estado")).getText()).isEqualTo("Bloqueada");
    }

    @Test
    void filtraPorGenYCategoriaEnMenosDeTresSegundos() { // RF-09, RNF búsquedas < 3 s
        String patogenica = "WEB-" + sufijo + "-F1";
        String benigna = "WEB-" + sufijo + "-F2";
        ingresar("tecnico_lab");
        registrar(patogenica, "1", "PATOGENICA");
        registrar(benigna, "1", "BENIGNA");
        ingresar("bio_user");
        abrir(patogenica);
        driver.findElement(By.id("btn-procesar")).click();
        esperarEstado("Pendiente de firma");
        abrir(benigna);
        driver.findElement(By.id("btn-procesar")).click();
        esperarEstado("Clasificada");

        escribir("filtro-gen", "brca1");
        new Select(driver.findElement(By.id("filtro-categoria"))).selectByVisibleText("PATOGENICA");
        long inicio = System.currentTimeMillis();
        driver.findElement(By.id("btn-filtrar")).click();
        espera.until(d -> !d.findElements(By.cssSelector("#tabla-muestras tr[data-codigo='" + patogenica + "']")).isEmpty()
            && d.findElements(By.cssSelector("#tabla-muestras tr[data-codigo='" + benigna + "']")).isEmpty());

        assertThat(System.currentTimeMillis() - inicio).isLessThan(3000);
    }
}
