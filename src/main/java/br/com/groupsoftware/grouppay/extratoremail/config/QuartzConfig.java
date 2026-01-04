package br.com.groupsoftware.grouppay.extratoremail.config;

import br.com.groupsoftware.grouppay.extratoremail.job.*;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do Quartz para agendamento de jobs no sistema de extração de e-mails e processamento de documentos.
 * <p>
 * Esta classe configura e registra os jobs necessários para o sistema, incluindo a extração de PDFs,
 * envio para o S3, processamento de e-mails e tarefas de limpeza de arquivos locais.
 * </p>
 * <p>
 * Os jobs são habilitados ou desabilitados com base nas propriedades definidas no arquivo de configuração.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Configuration
public class QuartzConfig {

    @Value("${quartz.jobs.jobDecryptPdf.enabled}")
    private boolean isJobDecryptPdfEnabled;

    @Value("${quartz.jobs.jobDeleteFromDownload.enabled}")
    private boolean isJobDeleteFromDownloadEnabled;

    @Value("${quartz.jobs.jobEmailProcess.enabled}")
    private boolean isJobEmailProcessEnabled;

    @Value("${quartz.jobs.jobExtractorExpense.enabled}")
    private boolean isJobExtractorExpenseEnabled;

    @Value("${quartz.jobs.jobSentToGroupPay.enabled}")
    private boolean isJobSentToGroupPayEnabled;

    @Value("${quartz.jobs.jobS3Upload.enabled}")
    private boolean isJobS3UploadEnabled;

    @Value("${quartz.jobs.jobS3Download.enabled}")
    private boolean isJobS3DownloadEnabled;

    @Value("${quartz.jobs.jobDeleteFromLocal.enabled}")
    private boolean isJobDeleteFromLocalEnabled;

    @Value("${quartz.jobs.jobExtractorText.enabled}")
    private boolean isJobExtractorTextEnabled;

    @Value("${quartz.jobs.jobUpdateCompany.enabled}")
    private boolean isJobUpdateCompanyEnabled;

    @Value("${quartz.jobs.jobResultGroupPay.enabled}")
    private boolean isJobResultGroupPayEnabled;

    @Value("${quartz.jobs.jobCompanyMatching.enabled}")
    private boolean isJobCompanyMatchingEnabled;

    //RESULT GROUP PAY
    @Bean
    public JobDetail jobResultGroupPay() {
        return createJobDetail(ResultGroupPayJob.class, isJobResultGroupPayEnabled, "jobResultGroupPay");
    }

    @Bean
    public Trigger triggerJobResultGroupPay(JobDetail jobResultGroupPay) {
        return createTrigger(jobResultGroupPay, "triggerJobResultGroupPay"); //30s
    }

    //UPDATE COMPANY
    @Bean
    public JobDetail jobUpdateCompany() {
        return createJobDetail(UpdateCompanyJob.class, isJobUpdateCompanyEnabled, "jobUpdateCompany");
    }

    @Bean
    public Trigger triggerJobUpdateCompany(JobDetail jobUpdateCompany) {
        return createTrigger(jobUpdateCompany, "triggerJobUpdateCompany", "0 */1 * * * ?"); //1m
    }

    //EXTRACTOR TEXT
    @Bean
    public JobDetail jobExtractorText() {
        return createJobDetail(ExtractorTextJob.class, isJobExtractorTextEnabled, "jobExtractorText");
    }

    @Bean
    public Trigger triggerExtractorText(JobDetail jobExtractorText) {
        return createTrigger(jobExtractorText, "triggerExtractorText");
    }

    //DELETE DOCUMENT FROM LOCAL
    @Bean
    public JobDetail jobDeleteFromLocal() {
        return createJobDetail(DeleteLocalDocumentJob.class, isJobDeleteFromLocalEnabled, "jobDeleteFromLocal");
    }

    @Bean
    public Trigger triggerDeleteFromLocal(JobDetail jobDeleteFromLocal) {
        return createTrigger(jobDeleteFromLocal, "triggerDeleteFromLocal", "0 */20 * * * ?"); //5m
    }

    //S3 UPLOAD
    @Bean
    public JobDetail jobS3Upload() {
        return createJobDetail(S3UploadJob.class, isJobS3UploadEnabled, "jobS3Upload");
    }

    @Bean
    public Trigger triggerS3Upload(JobDetail jobS3Upload) {
        return createTrigger(jobS3Upload, "triggerS3Upload");
    }

    //S3 DOWNLOAD
    @Bean
    public JobDetail jobS3Download() {
        return createJobDetail(S3DownloadJob.class, isJobS3DownloadEnabled, "jobS3Download");
    }

    @Bean
    public Trigger triggerS3Download(JobDetail jobS3Download) {
        return createTrigger(jobS3Download, "triggerS3Download", "0/10 * * * * ?"); //10s
    }

    //SENT EXPENSE
    @Bean
    public JobDetail jobSentToGroupPay() {
        return createJobDetail(SentToGroupPayJob.class, isJobSentToGroupPayEnabled, "jobSentToGroupPay");
    }

    @Bean
    public Trigger triggerSentToGroupPay(JobDetail jobSentToGroupPay) {
        return createTrigger(jobSentToGroupPay, "triggerSentToGroupPay");
    }

    //EXTRACTOR EXPENSE
    @Bean
    public JobDetail jobExtractorExpense() {
        return createJobDetail(ExtractorExpenseJob.class, isJobExtractorExpenseEnabled, "jobExtractorExpense");
    }

    @Bean
    public Trigger triggerExtractorExpense(JobDetail jobExtractorExpense) {
        return createTrigger(jobExtractorExpense, "triggerExtractorExpense", "0 */1 * * * ?"); //1m
    }

    //DECRYPT PDF
    @Bean
    public JobDetail jobDecryptPdf() {
        return createJobDetail(DecryptPdfJob.class, isJobDecryptPdfEnabled, "jobDecryptPdf");
    }

    @Bean
    public Trigger triggerDecryptPdf(JobDetail jobDecryptPdf) {
        return createTrigger(jobDecryptPdf, "triggerDecryptPdf");
    }

    //DELETE DOCUMENT FROM DOWNLOAD
    @Bean
    public JobDetail jobDeleteFromDownload() {
        return createJobDetail(DeleteDocumentDownloadJob.class, isJobDeleteFromDownloadEnabled, "jobDeleteFromDownload");
    }

    @Bean
    public Trigger triggerDeleteFromDownload(JobDetail jobDeleteFromDownload) {
        return createTrigger(jobDeleteFromDownload, "triggerDeleteFromDownload", "0 0/1 * * * ?"); //1h
    }

    //EMAIL PROCESS
    @Bean
    public JobDetail jobEmailProcess() {
        return createJobDetail(EmailProcessJob.class, isJobEmailProcessEnabled, "jobEmailProcess");
    }

    @Bean
    public Trigger triggerEmailProcess(JobDetail jobEmailProcess) {
        return createTrigger(jobEmailProcess, "triggerEmailProcess", "0/10 * * * * ?"); //10s
    }

    //COMPANY MATCHING
    @Bean
    public JobDetail jobCompanyMatching() {
        return createJobDetail(CompanyMatchingJob.class, isJobCompanyMatchingEnabled, "jobCompanyMatching");
    }

    @Bean
    public Trigger triggerCompanyMatching(JobDetail jobCompanyMatching) {
        return createTrigger(jobCompanyMatching, "triggerCompanyMatching"); //10s
    }

    private JobDetail createJobDetail(Class<? extends Job> jobClass, boolean isEnabled, String jobName) {
        if (isEnabled) {
            return JobBuilder.newJob(jobClass)
                    .withIdentity(jobName)
                    .storeDurably()
                    .build();
        }
        return null;
    }

    private Trigger createTrigger(JobDetail jobDetail, String triggerName) {
        return createTrigger(jobDetail, triggerName, "0/30 * * * * ?"); //A cada 30 segundos
    }

    private Trigger createTrigger(JobDetail jobDetail, String triggerName, String cronExpression) {
        if (jobDetail != null) {
            return TriggerBuilder.newTrigger()
                    .forJob(jobDetail)
                    .withIdentity(triggerName)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                    .build();
        }
        return null;
    }
}
