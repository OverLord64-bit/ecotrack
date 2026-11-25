package com.ecotrack.municipal.records;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

/** Tiny admin page: shows the records stored by the municipal system, proving it is a separate system. */
@RestController
public class AdminController {

    private final MunicipalRecordRepository repository;

    public AdminController(MunicipalRecordRepository repository) {
        this.repository = repository;
    }

    @GetMapping(value = {"/", "/admin"}, produces = MediaType.TEXT_HTML_VALUE)
    public String page() {
        StringBuilder rows = new StringBuilder();
        for (MunicipalRecord r : repository.findAllByOrderByReceivedAtDesc()) {
            rows.append("<tr><td>").append(HtmlUtils.htmlEscape(r.getReference()))
                .append("</td><td><code>").append(r.getRecordId())
                .append("</code></td><td><code>").append(r.getCollectionId())
                .append("</code></td><td>").append(r.getCompletedAt())
                .append("</td><td>").append(r.getReportId())
                .append("</td><td>").append(HtmlUtils.htmlEscape(r.getNotes() == null ? "" : r.getNotes()))
                .append("</td><td>").append(r.getReceivedAt())
                .append("</td></tr>");
        }
        String body = rows.length() == 0 ? "<tr><td colspan=7>No records yet.</td></tr>" : rows.toString();
        return """
            <!doctype html><html lang="en"><head><meta charset="utf-8">
            <meta http-equiv="refresh" content="5">
            <title>Municipal Information System</title>
            <style>
              body{font-family:system-ui,sans-serif;margin:2rem;color:#1f2937;background:#f9fafb}
              p{color:#6b7280}
              table{border-collapse:collapse;width:100%;background:#fff}
              th,td{border:1px solid #e5e7eb;padding:.5rem .7rem;text-align:left;font-size:.85rem}
              th{background:#ecfdf5}
            </style></head><body>
            <h1>Municipal Information System</h1>
            <p>Records received over SOAP from EcoTrack (refreshes every 5 s). WSDL: <a href="/ws/municipal.wsdl">/ws/municipal.wsdl</a></p>
            <table><tr><th>Reference</th><th>MunicipalRecord ID</th><th>Collection ID</th><th>Completed at</th>
            <th>Report</th><th>Notes</th><th>Received at</th></tr>""" + body + "</table></body></html>";
    }
}
