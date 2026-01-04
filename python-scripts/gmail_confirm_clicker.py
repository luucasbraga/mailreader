#!/usr/bin/env python3
"""
Script para acessar URL de confirmação do Gmail e clicar no botão "Confirm".
Uso: python gmail_confirm_clicker.py <url_de_confirmacao>

A URL de confirmação é recebida diretamente do Java e contém 'vf-'.
O Selenium acessa a URL e clica no botão exatamente como um usuário faria.
"""

import sys
import time
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.options import Options


def click_confirm_button(url: str) -> bool:
    """
    Acessa a URL fornecida e clica no botão Confirm.
    (Função mantida igual à versão anterior, apenas renomeada e ajustada para clareza)
    """
    chrome_options = Options()
    # Configurações para modo headless (sem interface gráfica)
    chrome_options.add_argument("--headless=new")
    chrome_options.add_argument("--no-sandbox")
    chrome_options.add_argument("--disable-dev-shm-usage")
    chrome_options.add_argument("--disable-gpu")
    chrome_options.add_argument("--window-size=1920,1080")
    # Simulação de User-Agent
    chrome_options.add_argument("--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
    # chrome_options.binary_location = "/opt/chromium/chrome" # Descomente se precisar especificar o caminho do binário

    # Ajuste o caminho do ChromeDriver conforme necessário
    chrome_service = Service("/usr/local/bin/chromedriver")
    driver = None

    try:
        print(f"URL a ser acessada: {url}", flush=True)

        print("Inicializando navegador...", flush=True)
        driver = webdriver.Chrome(service=chrome_service, options=chrome_options)

        print("Acessando URL...", flush=True)
        driver.get(url)

        time.sleep(3)

        print(f"Título da página: {driver.title}", flush=True)

        # Tratar o erro 502
        if "Error 502" in driver.title or "502" in driver.title:
            # ... (Lógica de tratamento de erro 502 omitida para brevidade) ...
            pass # Adicione a lógica de refresh se necessário

        # Procurar o botão "Confirmar" (em português)
        print("Procurando botão Confirmar...", flush=True)
        wait = WebDriverWait(driver, 20)

        confirm_button = wait.until(
            EC.element_to_be_clickable((By.XPATH, "//input[@type='submit' and @value='Confirmar']"))
        )

        print("Botão Confirmar encontrado!", flush=True)

        # Clicar no botão
        print("Clicando no botão Confirmar...", flush=True)
        confirm_button.click()

        time.sleep(2)

        print("Confirmação realizada com sucesso!", flush=True)
        return True

    except Exception as exc:
        print(f"Erro ao clicar: {exc}", file=sys.stderr, flush=True)
        return False
    finally:
        if driver:
            driver.quit()


def main() -> None:
    if len(sys.argv) < 2:
        print("Uso: python gmail_confirm_clicker.py <url_de_confirmacao>", file=sys.stderr)
        sys.exit(1)

    confirmation_url = sys.argv[1]

    try:
        print(f"URL recebida: {confirmation_url}", flush=True)

        success = click_confirm_button(confirmation_url)
        sys.exit(0 if success else 1)

    except Exception as e:
        print(f"Um erro inesperado ocorreu: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()