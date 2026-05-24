# Power Trade Test Data Generator
# Generates 20 power trading policy documents

$OutputDir = "data/test-documents"

# Create output directory
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
    Write-Host "[OK] Created directory: $OutputDir"
}

# Document titles (20 documents)
$DocumentTitles = @(
    "Power Medium Long Term Trading Rules",
    "Power Spot Market Trading Rules",
    "Power Ancillary Service Management Measures",
    "Renewable Energy Power Consumption Guarantee Mechanism",
    "Power Market Information Disclosure Management Measures",
    "Power Retail Market Trading Rules",
    "Power Wholesale Market Supervision Measures",
    "Cross Provincial and Regional Power Trading Rules",
    "Power Market Settlement Management Measures",
    "Power Generation Enterprise Grid Connection Operation Management Regulations",
    "Power User Participation in Market Trading Implementation Rules",
    "Power Sales Company Management Measures",
    "Power Market Risk Prevention and Control Guidelines",
    "Power Trading Institution Establishment and Standardized Operation Measures",
    "Power Market Order Supervision Interim Measures",
    "New Energy Participation in Power Market Trading Guidance Opinions",
    "Power Demand Side Management Measures",
    "Power Market Credit System Construction Guidance Opinions",
    "Power Market Operation Rules",
    "Power Trading Contract Demonstration Text"
)

# Agencies
$Agencies = @(
    "National Energy Administration",
    "National Development and Reform Commission",
    "Ministry of Industry and Information Technology",
    "Provincial Energy Bureaus",
    "Power Trading Center",
    "Power Grid Corporation"
)

# Chapter titles
$ChapterTitles = @(
    "General Provisions",
    "Market Members",
    "Trading Varieties",
    "Trading Methods",
    "Price Mechanism",
    "Measurement and Settlement",
    "Information Disclosure",
    "Risk Prevention and Control",
    "Supervision and Management",
    "Supplementary Provisions"
)

# Article templates
$ArticleTemplates = @(
    "Power trading institutions shall organize market members to carry out trading activities in accordance with the principles of fairness, justice and openness.",
    "Market members shall comply with power market trading rules and fulfill relevant information disclosure obligations.",
    "Power generation enterprises, power users and power sales companies shall participate in power market trading in accordance with regulations.",
    "Power dispatching institutions shall ensure the safe and stable operation of the power grid and safeguard the legitimate rights and interests of all parties.",
    "Trading prices shall reflect the relationship between power supply and demand and promote the optimal allocation of resources.",
    "Market members shall establish and improve risk management systems to prevent market risks.",
    "Power trading institutions shall release market information in a timely manner and accept social supervision.",
    "Those who violate these provisions shall be ordered by the energy department to make corrections and be punished in accordance with the law.",
    "Renewable energy power generation enterprises are encouraged to participate in power market trading.",
    "Power market trading shall be conducted electronically to improve trading efficiency.",
    "Market entities shall be honest and trustworthy and maintain a good market order.",
    "Establish a power market credit evaluation system and implement credit classification supervision.",
    "Power trading contracts shall clearly agree on trading electricity, electricity prices, settlement methods and other contents.",
    "Cross provincial and regional transactions shall comply with national energy strategies and power development plans.",
    "Power ancillary service compensation fees are included in transmission and distribution price recovery."
)

# Document objects array
$Documents = @()

Write-Host ""
Write-Host "==========================================="
Write-Host "Generating 20 Power Trading Test Documents"
Write-Host "==========================================="
Write-Host ""

# Generate 20 documents
for ($i = 0; $i -lt 20; $i++) {
    $index = $i + 1
    $title = $DocumentTitles[$i]
    $agency = $Agencies[$i % $Agencies.Length]
    $docNumber = $agency.Substring(0,3) + "-202" + ($i % 5) + "-" + "{0:D3}" -f $index
    
    # Publish date
    $year = 2020 + ($i % 5)
    $month = 1 + ($i % 12)
    $day = 1 + ($i % 28)
    $publishDate = "$year-$($month.ToString('00'))-$($day.ToString('00'))"
    
    # Implement date
    $implementYear = $year
    $implementMonth = $month + 1 + ($i % 3)
    if ($implementMonth -gt 12) {
        $implementMonth -= 12
        $implementYear++
    }
    $implementDate = "$implementYear-$($implementMonth.ToString('00'))-$($day.ToString('00'))"
    
    $docId = "DOC{0:D3}" -f $index
    $fileName = "$docId-$($title.Replace(' ','').Substring(0, [Math]::Min(8, $title.Length))).txt"
    
    # Generate document content
    $content = "$agency Notice on Printing and Distributing $title`n`n"
    $content += "$docNumber`n`n"
    $content += "========================================`n"
    $content += "$publishDate`n"
    $content += "========================================`n`n"
    
    # Generate 10 chapters
    for ($chapter = 0; $chapter -lt 10; $chapter++) {
        $content += "Chapter $($chapter + 1): $($ChapterTitles[$chapter])`n`n"
        
        $articlesCount = 3 + ($chapter % 3)
        for ($article = 0; $article -lt $articlesCount; $article++) {
            $articleNum = $chapter * 5 + $article + 1
            $content += "Article $articleNum "
            $content += $ArticleTemplates[($chapter + $article) % $ArticleTemplates.Length] + "`n`n"
        }
    }
    
    # Supplementary provisions
    $validYears = 3 + ($docId.GetHashCode() % 5)
    $content += "`n========================================`n"
    $content += "These rules shall come into force on $implementDate and shall be valid for $validYears years.`n"
    $content += "========================================`n"
    
    # Save file
    $filePath = "$OutputDir\$fileName"
    $content | Out-File -FilePath $filePath -Encoding UTF8
    
    # Create document object
    $doc = @{
        doc_id = $docId
        kb_id = "KB001"
        title = "$agency Notice on Printing and Distributing $title"
        file_name = $fileName
        doc_number = $docNumber
        publish_date = $publishDate
        implement_date = $implementDate
        doc_type = "POLICY"
        file_size = $content.Length
        tags = @("Power Trading", "Market Rules")
    }
    
    # Add specific tags
    if ($index -le 5) { $doc.tags += @("Medium Long Term Trading") }
    elseif ($index -le 10) { $doc.tags += @("Spot Market") }
    elseif ($index -le 15) { $doc.tags += @("Ancillary Services") }
    else { $doc.tags += @("Renewable Energy") }
    $doc.tags += @("Policy Regulations")
    
    $doc.summary = "This document stipulates $($doc.title), clarifies the rights and obligations of market members, trading methods, price mechanisms, settlement management, etc., comes into force on $implementDate, and is valid for $validYears years."
    
    $Documents += $doc
    
    Write-Host "[OK] Generated document [$($index.ToString('00'))/20]: $fileName"
}

Write-Host ""
Write-Host "[OK] Documents generated successfully"

# Generate SQL import script
Write-Host ""
Write-Host "Generating SQL import script..."

$sql = @"
-- ==========================================
-- Power Trading Test Data Import Script
-- Generated: $(Get-Date -Format "yyyy-MM-dd")
-- Total Documents: 20
-- ==========================================

USE power_trade_rag;

-- Insert document metadata
"@

foreach ($doc in $Documents) {
    $sql += "`nINSERT INTO document_info (doc_id, kb_id, title, file_name, file_size, doc_type, status, create_time) "
    $sql += "VALUES ('$($doc.doc_id)', '$($doc.kb_id)', '$($doc.title -replace "'", "''")', '$($doc.file_name -replace "'", "''")', $($doc.file_size), '$($doc.doc_type)', 1, NOW());"
}

$sql += @"


-- Insert knowledge base information
INSERT INTO knowledge_base (kb_id, name, description, status, create_time) VALUES ('KB001', 'Power Trading Policy Database', 'Collection of national and local power trading policy documents', 1, NOW());
INSERT INTO knowledge_base (kb_id, name, description, status, create_time) VALUES ('KB002', 'Power Market Trading Rules Database', 'Collection of various power market trading rules and implementation details', 1, NOW());
INSERT INTO knowledge_base (kb_id, name, description, status, create_time) VALUES ('KB003', 'Power Ancillary Service Management Database', 'Collection of power ancillary service management measures and regulations', 1, NOW());
"@

$sql | Out-File -FilePath "$OutputDir/import_test_data.sql" -Encoding UTF8
Write-Host "[OK] SQL script generated"

# Generate JSON metadata
Write-Host "Generating JSON metadata file..."

$json = @"
{
  "generated_date": "$(Get-Date -Format "yyyy-MM-dd")",
  "total_documents": 20,
  "description": "Power Trading Rules and Policy Test Dataset",
  "documents": [
"@

for ($i = 0; $i -lt $Documents.Count; $i++) {
    $doc = $Documents[$i]
    $json += @"
    {
      "doc_id": "$($doc.doc_id)",
      "kb_id": "$($doc.kb_id)",
      "title": "$($doc.title -replace '"', '\"')",
      "file_name": "$($doc.file_name -replace '"', '\"')",
      "doc_number": "$($doc.doc_number -replace '"', '\"')",
      "publish_date": "$($doc.publish_date)",
      "implement_date": "$($doc.implement_date)",
      "doc_type": "$($doc.doc_type)",
      "file_size": $($doc.file_size),
      "tags": ["$($doc.tags -join '","')"],
      "summary": "$($doc.summary -replace '"', '\"')"
    }
"@
    if ($i -lt $Documents.Count - 1) { $json += "," }
    $json += "`n"
}

$json += @"
  ]
}
"@

$json | Out-File -FilePath "$OutputDir/documents_metadata.json" -Encoding UTF8
Write-Host "[OK] JSON metadata generated"

Write-Host ""
Write-Host "==========================================="
Write-Host "Test Data Generation Complete!"
Write-Host "==========================================="
Write-Host "Output Directory: $OutputDir"
Write-Host "Total Documents: 20"
Write-Host "Knowledge Base: KB001 (Power Trading Policy Database)"
Write-Host ""
