#!/bin/bash
echo "📊 JaCoCo Test Coverage Summary"
echo "==============================="
echo ""

if [ -f "target/site/jacoco/jacoco.csv" ]; then
    echo "🎯 Overall Coverage:"
    echo "===================="

    # Calcola coverage totale da jacoco.csv
    tail -n +2 target/site/jacoco/jacoco.csv | awk -F',' '
    {
        instruction_missed += $4
        instruction_covered += $5
        branch_missed += $6
        branch_covered += $7
        line_missed += $8
        line_covered += $9
        method_missed += $11
        method_covered += $12
    }
    END {
        instruction_total = instruction_missed + instruction_covered
        branch_total = branch_missed + branch_covered
        line_total = line_missed + line_covered
        method_total = method_missed + method_covered

        instruction_coverage = instruction_total > 0 ? (instruction_covered / instruction_total) * 100 : 0
        branch_coverage = branch_total > 0 ? (branch_covered / branch_total) * 100 : 0
        line_coverage = line_total > 0 ? (line_covered / line_total) * 100 : 0
        method_coverage = method_total > 0 ? (method_covered / method_total) * 100 : 0

        printf "📈 Instructions: %.1f%% (%d/%d)\n", instruction_coverage, instruction_covered, instruction_total
        printf "🌿 Branches:     %.1f%% (%d/%d)\n", branch_coverage, branch_covered, branch_total
        printf "📝 Lines:        %.1f%% (%d/%d)\n", line_coverage, line_covered, line_total
        printf "⚙️  Methods:      %.1f%% (%d/%d)\n", method_coverage, method_covered, method_total
    }'

    echo ""
    echo "📊 Coverage by Package:"
    echo "======================="

    # Coverage per package
    tail -n +2 target/site/jacoco/jacoco.csv | awk -F',' '
    {
        package = $2
        instruction_missed[package] += $4
        instruction_covered[package] += $5
    }
    END {
        for (package in instruction_missed) {
            total = instruction_missed[package] + instruction_covered[package]
            coverage = total > 0 ? (instruction_covered[package] / total) * 100 : 0
            printf "%-40s %.1f%%\n", package, coverage
        }
    }' | sort -k2 -nr

    echo ""
    echo "🎯 Best Covered Classes:"
    echo "========================"

    # Top 5 classi meglio coperte
    tail -n +2 target/site/jacoco/jacoco.csv | awk -F',' '
    {
        class = $3
        instruction_missed = $4
        instruction_covered = $5
        total = instruction_missed + instruction_covered

        if (total > 50) {  # Solo classi con almeno 50 istruzioni
            coverage = total > 0 ? (instruction_covered / total) * 100 : 0
            printf "%.1f%%\t%s\n", coverage, class
        }
    }' | sort -nr | head -5

    echo ""
    echo "🔍 Classes Needing Attention:"
    echo "=============================="

    # Classi con bassa coverage
    tail -n +2 target/site/jacoco/jacoco.csv | awk -F',' '
    {
        class = $3
        instruction_missed = $4
        instruction_covered = $5
        total = instruction_missed + instruction_covered

        if (total > 50) {  # Solo classi con almeno 50 istruzioni
            coverage = total > 0 ? (instruction_covered / total) * 100 : 0
            if (coverage < 50) {
                printf "%.1f%%\t%s\n", coverage, class
            }
        }
    }' | sort -n | head -10

else
    echo "❌ JaCoCo coverage report not found!"
    echo "Run 'mvn clean test' to generate coverage reports."
fi

echo ""
echo "📁 Reports Location:"
echo "==================="
echo "HTML Report: target/site/jacoco/index.html"
echo "XML Report:  target/site/jacoco/jacoco.xml"
echo "CSV Report:  target/site/jacoco/jacoco.csv"
echo ""
echo "🚀 To view HTML report: open target/site/jacoco/index.html in your browser"