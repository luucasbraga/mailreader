package br.com.groupsoftware.grouppay.extratoremail.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Classe para executar o script Python que clica no botão Confirm do Gmail
 */
public class GmailConfirmExecutor {

    private static final String[] POSSIBLE_PATHS = {
        "python-scripts/gmail_confirm_clicker.py",
        "/usr/local/tomcat/webapps/python-scripts/gmail_confirm_clicker.py",
        "/usr/local/tomcat/python-scripts/gmail_confirm_clicker.py",
        System.getProperty("user.dir") + "/python-scripts/gmail_confirm_clicker.py"
    };

    /**
     * Encontra o caminho do script Python tentando vários locais possíveis
     *
     * @return caminho absoluto do script se encontrado, null caso contrário
     */
    private static String encontrarScriptPython() {
        for (String path : POSSIBLE_PATHS) {
            try {
                File scriptFile = new File(path);
                if (scriptFile.exists() && scriptFile.isFile()) {
                    String absolutePath = scriptFile.getCanonicalPath();
                    System.out.println("Script Python encontrado em: " + absolutePath);
                    return absolutePath;
                }
            } catch (IOException e) {
                System.out.println("Caminho não acessível: " + path + " - " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Executa o script Python para clicar no botão de confirmação
     *
     * @param url URL da página de confirmação do Gmail
     * @return true se a execução foi bem-sucedida, false caso contrário
     */
    public static boolean executarConfirmacao(String url) {
        try {

            String absoluteScriptPath = encontrarScriptPython();
            
            if (absoluteScriptPath == null) {
                System.err.println("Erro: Script Python não encontrado. Tentou os seguintes caminhos:");
                for (String path : POSSIBLE_PATHS) {
                    System.err.println("  - " + path);
                }
                return false;
            }
            
            System.out.println("Executando script Python: " + absoluteScriptPath);
            
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "python3",
                    absoluteScriptPath,
                    url
            );


            System.out.println("Iniciando processo Python...");
            Process process = processBuilder.start();
            
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String linha;
                    while ((linha = reader.readLine()) != null) {
                        System.out.println("[PYTHON] " + linha);
                        output.append(linha).append("\n");
                    }
                } catch (IOException e) {
                    System.err.println("Erro ao ler saída do script: " + e.getMessage());
                }
            });
            
            Thread errorThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
                    String linha;
                    while ((linha = reader.readLine()) != null) {
                        System.err.println("[PYTHON-ERROR] " + linha);
                        errorOutput.append(linha).append("\n");
                    }
                } catch (IOException e) {
                    System.err.println("Erro ao ler erro do script: " + e.getMessage());
                }
            });
            
            outputThread.start();
            errorThread.start();

            System.out.println("Aguardando conclusão do script (timeout: 60s)...");
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            
            if (!finished) {
                System.err.println("Script Python excedeu o timeout de 60 segundos. Encerrando processo...");
                process.destroyForcibly();
                outputThread.join(2000);
                errorThread.join(2000);
                System.err.println("Saída capturada até o timeout:");
                System.err.println(output.toString());
                System.err.println("Erros capturados:");
                System.err.println(errorOutput.toString());
                return false;
            }
            
            outputThread.join(2000);
            errorThread.join(2000);
            
            int codigoSaida = process.exitValue();

            if (codigoSaida == 0) {
                System.out.println("Script executado com sucesso!");
                return true;
            } else {
                System.err.println("Script falhou com código: " + codigoSaida);
                return false;
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Erro ao executar script: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Método main para teste
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: java GmailConfirmExecutor <url_da_pagina>");
            System.exit(1);
        }

        String url = args[0];
        boolean sucesso = executarConfirmacao(url);

        System.exit(sucesso ? 0 : 1);
    }
}
