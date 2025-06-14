
class BankStatsPDFGenerator {
    constructor() {
        this.colors = {
            primary: [59, 130, 246],    
            secondary: [99, 102, 241],
            accent: [16, 185, 129],
            purple: [168, 85, 247],
            text: [55, 65, 81],
            lightGray: [156, 163, 175],
            background: [248, 250, 252]
        };
    }

    async generatePDF() {
        try {

            this.showLoading(true);


            const stats = await this.fetchStatistics();
            const accounts = await this.fetchAccounts();
            

            const { jsPDF } = window.jspdf;
            const doc = new jsPDF({
                orientation: 'portrait',
                unit: 'mm',
                format: 'a4'
            });


            this.setupDocument(doc);
            

            let yPosition = this.addHeader(doc, 20);
            yPosition = this.addExecutiveSummary(doc, stats, yPosition + 20);
            yPosition = this.addStatisticsCards(doc, stats, yPosition + 15);
            yPosition = this.addAccountDistribution(doc, stats, yPosition + 20);
            yPosition = this.addAccountsList(doc, accounts, yPosition + 20);
            this.addFooter(doc);


            const filename = `bank_statistics_report_${new Date().toISOString().split('T')[0]}.pdf`;
            doc.save(filename);


            this.showSuccess('PDF report generated successfully!');
            
        } catch (error) {
            console.error('Error generating PDF:', error);
            this.showError('Failed to generate PDF report. Please try again.');
        } finally {
            this.showLoading(false);
        }
    }

    async fetchStatistics() {
        const response = await fetch('/comptes/api/stats');
        if (!response.ok) {
            throw new Error('Failed to fetch statistics');
        }
        return await response.json();
    }

    async fetchAccounts() {
        const response = await fetch('/comptes/api/accounts');
        if (!response.ok) {
            throw new Error('Failed to fetch accounts');
        }
        return await response.json();
    }

    setupDocument(doc) {
        doc.setFont('helvetica');
    }

    addHeader(doc, y) {
        doc.setFontSize(28);
        doc.setFont('helvetica', 'bold');
        doc.setTextColor(...this.colors.primary);
        doc.text('BankDash Statistics Report', 105, y, { align: 'center' });

        const now = new Date();
        const dateStr = now.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
        
        doc.setFontSize(12);
        doc.setFont('helvetica', 'normal');
        doc.setTextColor(...this.colors.text);
        doc.text(`Generated on ${dateStr}`, 105, y + 10, { align: 'center' });

        return y + 10;
    }

    addExecutiveSummary(doc, stats, y) {
        doc.setFontSize(18);
        doc.setFont('helvetica', 'bold');
        doc.setTextColor(...this.colors.secondary);
        doc.text('Executive Summary', 20, y);
        
        return y;
    }

    addStatisticsCards(doc, stats, y) {
        const cardWidth = 85;
        const cardHeight = 35;
        const margin = 10;
        
        this.drawStatCard(doc, 20, y, cardWidth, cardHeight, 'Total Accounts', 
                         stats.totalAccounts?.toString() || '0', this.colors.primary);
        
        this.drawStatCard(doc, 20 + cardWidth + margin, y, cardWidth, cardHeight, 'Total Balance', 
                         `€${stats.totalBalance || '0'}`, this.colors.accent);
        
        const secondRowY = y + cardHeight + margin;
        this.drawStatCard(doc, 20, secondRowY, cardWidth, cardHeight, 'Average Balance', 
                         `€${stats.averageBalance || '0'}`, this.colors.secondary);
        
        this.drawStatCard(doc, 20 + cardWidth + margin, secondRowY, cardWidth, cardHeight, 'Total Operations', 
                         stats.totalOperations?.toString() || '0', this.colors.purple);
        
        return secondRowY + cardHeight;
    }

    drawStatCard(doc, x, y, width, height, title, value, color) {
        doc.setFillColor(...this.colors.background);
        doc.rect(x, y, width, height, 'F');
        
        doc.setDrawColor(229, 231, 235);
        doc.setLineWidth(0.5);
        doc.rect(x, y, width, height, 'S');
        
        doc.setFontSize(10);
        doc.setFont('helvetica', 'normal');
        doc.setTextColor(107, 114, 128);
        doc.text(title, x + width/2, y + 12, { align: 'center' });
        
        doc.setFontSize(20);
        doc.setFont('helvetica', 'bold');
        doc.setTextColor(...color);
        doc.text(value, x + width/2, y + 25, { align: 'center' });
    }

    addAccountDistribution(doc, stats, y) {
        if (!stats.accountsByType || Object.keys(stats.accountsByType).length === 0) {
            return y;
        }

        doc.setFontSize(18);
        doc.setFont('helvetica', 'bold');
        doc.setTextColor(...this.colors.secondary);
        doc.text('Account Type Distribution', 20, y);
        
        y += 15;
        
        const tableX = 20;
        const colWidths = [60, 40, 40];
        const rowHeight = 10;
        
        doc.setFillColor(...this.colors.primary);
        doc.rect(tableX, y, colWidths.reduce((a, b) => a + b), rowHeight, 'F');
        
        doc.setFontSize(10);
        doc.setFont('helvetica', 'bold');
        doc.setTextColor(255, 255, 255);
        doc.text('Account Type', tableX + colWidths[0]/2, y + 6, { align: 'center' });
        doc.text('Count', tableX + colWidths[0] + colWidths[1]/2, y + 6, { align: 'center' });
        doc.text('Percentage', tableX + colWidths[0] + colWidths[1] + colWidths[2]/2, y + 6, { align: 'center' });
        
        y += rowHeight;
        
        const total = Object.values(stats.accountsByType).reduce((sum, count) => sum + count, 0);
        
        doc.setFont('helvetica', 'normal');
        doc.setTextColor(...this.colors.text);
        
        Object.entries(stats.accountsByType).forEach(([type, count], index) => {
            const percentage = total > 0 ? ((count / total) * 100).toFixed(1) : '0.0';
            
            if (index % 2 === 0) {
                doc.setFillColor(249, 250, 251);
                doc.rect(tableX, y, colWidths.reduce((a, b) => a + b), rowHeight, 'F');
            }
            
            doc.setDrawColor(229, 231, 235);
            doc.setLineWidth(0.2);
            doc.rect(tableX, y, colWidths.reduce((a, b) => a + b), rowHeight, 'S');
            
            doc.text(this.getAccountTypeLabel(type), tableX + colWidths[0]/2, y + 6, { align: 'center' });
            doc.text(count.toString(), tableX + colWidths[0] + colWidths[1]/2, y + 6, { align: 'center' });
            doc.text(`${percentage}%`, tableX + colWidths[0] + colWidths[1] + colWidths[2]/2, y + 6, { align: 'center' });
            
            y += rowHeight;
        });
        
        return y;
    }

    addAccountsList(doc, accounts, y) {
        if (!accounts || accounts.length === 0) {
            return y;
        }

        // Check if we need a new page
        if (y > 200) {
            doc.addPage();
            y = 20;
        }

        doc.setFontSize(18);
        doc.setFont('helvetica', 'bold');
        doc.setTextColor(...this.colors.secondary);
        doc.text('Account Details', 20, y);
        
        y += 15;
        
        const tableX = 20;
        const colWidths = [35, 45, 35, 25, 30];
        const rowHeight = 8;
        
        // Table header
        doc.setFillColor(...this.colors.primary);
        doc.rect(tableX, y, colWidths.reduce((a, b) => a + b), rowHeight, 'F');
        
        doc.setFontSize(9);
        doc.setFont('helvetica', 'bold');
        doc.setTextColor(255, 255, 255);
        doc.text('Account Number', tableX + colWidths[0]/2, y + 5, { align: 'center' });
        doc.text('Account Holder', tableX + colWidths[0] + colWidths[1]/2, y + 5, { align: 'center' });
        doc.text('Balance', tableX + colWidths[0] + colWidths[1] + colWidths[2]/2, y + 5, { align: 'center' });
        doc.text('Type', tableX + colWidths[0] + colWidths[1] + colWidths[2] + colWidths[3]/2, y + 5, { align: 'center' });
        doc.text('Created', tableX + colWidths[0] + colWidths[1] + colWidths[2] + colWidths[3] + colWidths[4]/2, y + 5, { align: 'center' });
        
        y += rowHeight;
        
        doc.setFont('helvetica', 'normal');
        doc.setTextColor(...this.colors.text);
        
        accounts.forEach((account, index) => {
            // Check if we need a new page
            if (y > 270) {
                doc.addPage();
                y = 20;
            }
            
            if (index % 2 === 0) {
                doc.setFillColor(249, 250, 251);
                doc.rect(tableX, y, colWidths.reduce((a, b) => a + b), rowHeight, 'F');
            }
            
            doc.setDrawColor(229, 231, 235);
            doc.setLineWidth(0.2);
            doc.rect(tableX, y, colWidths.reduce((a, b) => a + b), rowHeight, 'S');
            
            // Format data
            const accountNumber = account.numeroCompte || 'N/A';
            const accountHolder = account.nomTitulaire || 'N/A';
            const balance = account.solde ? `€${parseFloat(account.solde).toFixed(2)}` : '€0.00';
            const accountType = this.getAccountTypeLabel(account.typeCompte) || 'N/A';
            const createdDate = account.dateCreation ? new Date(account.dateCreation).toLocaleDateString('en-GB') : 'N/A';
            
            doc.setFontSize(8);
            doc.text(accountNumber, tableX + colWidths[0]/2, y + 5, { align: 'center' });
            doc.text(accountHolder.length > 15 ? accountHolder.substring(0, 15) + '...' : accountHolder, tableX + colWidths[0] + colWidths[1]/2, y + 5, { align: 'center' });
            doc.text(balance, tableX + colWidths[0] + colWidths[1] + colWidths[2]/2, y + 5, { align: 'center' });
            doc.text(accountType, tableX + colWidths[0] + colWidths[1] + colWidths[2] + colWidths[3]/2, y + 5, { align: 'center' });
            doc.text(createdDate, tableX + colWidths[0] + colWidths[1] + colWidths[2] + colWidths[3] + colWidths[4]/2, y + 5, { align: 'center' });
            
            y += rowHeight;
        });
        
        return y;
    }

    addFooter(doc) {
        const pageHeight = doc.internal.pageSize.height;
        doc.setFontSize(8);
        doc.setFont('helvetica', 'normal');
        doc.setTextColor(...this.colors.lightGray);
        doc.text('This report was automatically generated by BankDash System', 
                105, pageHeight - 15, { align: 'center' });
    }

    getAccountTypeLabel(type) {
        const labels = {
            'COURANT': 'Current Account',
            'EPARGNE': 'Savings Account',
            'CREDIT': 'Credit Account'
        };
        return labels[type] || type;
    }

    showLoading(show) {
        const button = document.getElementById('pdf-download-btn');
        if (button) {
            if (show) {
                button.disabled = true;
                button.innerHTML = `
                    <svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Generating PDF...
                `;
            } else {
                button.disabled = false;
                button.innerHTML = `
                    <i data-lucide="file-text" class="w-5 h-5 mr-3"></i>
                    <span class="font-medium">Download PDF Report</span>
                `;  
                if (window.lucide) {
                    window.lucide.createIcons();
                }
            }
        }
    }

    showSuccess(message) {
        this.showNotification(message, 'success');
    }

    showError(message) {
        this.showNotification(message, 'error');
    }

    showNotification(message, type) {
        const notification = document.createElement('div');
        notification.className = `fixed top-4 right-4 p-4 rounded-lg shadow-lg z-50 transition-all duration-300 transform translate-x-full`;
        
        if (type === 'success') {
            notification.className += ' bg-green-500 text-white';
        } else {
            notification.className += ' bg-red-500 text-white';
        }
        
        notification.innerHTML = `
            <div class="flex items-center">
                <span>${message}</span>
                <button class="ml-4 text-white hover:text-gray-200" onclick="this.parentElement.parentElement.remove()">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                    </svg>
                </button>
            </div>
        `;
        
        document.body.appendChild(notification);
        
        setTimeout(() => {
            notification.classList.remove('translate-x-full');
        }, 100);
        
        setTimeout(() => {
            notification.classList.add('translate-x-full');
            setTimeout(() => {
                if (notification.parentElement) {
                    notification.remove();
                }
            }, 300);
        }, 5000);
    }
}

const pdfGenerator = new BankStatsPDFGenerator();

window.generateStatsPDF = () => pdfGenerator.generatePDF();