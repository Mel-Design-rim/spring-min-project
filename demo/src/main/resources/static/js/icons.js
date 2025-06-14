const iconCache = new Map();

const ICON_BASE_PATH = '/icons/';
async function loadIcon(iconName) {
    if (iconCache.has(iconName)) {
        return iconCache.get(iconName);
    }

    try {
        const response = await fetch(`${ICON_BASE_PATH}${iconName}.svg`);
        if (!response.ok) {
            throw new Error(`Failed to load icon: ${iconName}`);
        }
        
        const svgContent = await response.text();
        iconCache.set(iconName, svgContent);
        return svgContent;
    } catch (error) {
        console.warn(`Icon '${iconName}' not found, using fallback`);
        return getFallbackIcon();
    }
}


function getFallbackIcon() {
    return `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" x2="12" y1="8" y2="12"/><line x1="12" x2="12.01" y1="16" y2="16"/></svg>`;
}


async function replaceIcon(element) {
    const iconName = element.getAttribute('data-lucide');
    if (!iconName) return;

    try {
        const svgContent = await loadIcon(iconName);
        
        const tempDiv = document.createElement('div');
        tempDiv.innerHTML = svgContent;
        const svgElement = tempDiv.querySelector('svg');
        
        if (svgElement) {
            const classes = element.className;
            svgElement.className = classes;
            
            if (element.style.cssText) {
                svgElement.style.cssText = element.style.cssText;
            }
            
            Array.from(element.attributes).forEach(attr => {
                if (attr.name !== 'data-lucide' && attr.name !== 'class' && attr.name !== 'style') {
                    svgElement.setAttribute(attr.name, attr.value);
                }
            });
            
            element.parentNode.replaceChild(svgElement, element);
        }
    } catch (error) {
        console.error(`Failed to replace icon '${iconName}':`, error);
    }
}


async function initializeLocalIcons() {
    const iconElements = document.querySelectorAll('[data-lucide]');
    
    const batchSize = 10;
    for (let i = 0; i < iconElements.length; i += batchSize) {
        const batch = Array.from(iconElements).slice(i, i + batchSize);
        await Promise.all(batch.map(element => replaceIcon(element)));
    }
    
    console.log(`Replaced ${iconElements.length} Lucide icons with local versions`);
}


async function createIcon(iconName, className = '') {
    try {
        const svgContent = await loadIcon(iconName);
        const tempDiv = document.createElement('div');
        tempDiv.innerHTML = svgContent;
        const svgElement = tempDiv.querySelector('svg');
        
        if (svgElement && className) {
            svgElement.className = className;
        }
        
        return svgElement;
    } catch (error) {
        console.error(`Failed to create icon '${iconName}':`, error);
        return null;
    }
}


function observeIconChanges() {
    const observer = new MutationObserver((mutations) => {
        mutations.forEach((mutation) => {
            mutation.addedNodes.forEach((node) => {
                if (node.nodeType === Node.ELEMENT_NODE) {
                    if (node.hasAttribute && node.hasAttribute('data-lucide')) {
                        replaceIcon(node);
                    }
                    
                    if (node.querySelectorAll) {
                        const iconElements = node.querySelectorAll('[data-lucide]');
                        iconElements.forEach(element => replaceIcon(element));
                    }
                }
            });
        });
    });
    
    observer.observe(document.body, {
        childList: true,
        subtree: true
    });
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        initializeLocalIcons();
        observeIconChanges();
    });
} else {
    initializeLocalIcons();
    observeIconChanges();
}

window.LocalIcons = {
    loadIcon,
    createIcon,
    initializeLocalIcons,
    replaceIcon
};