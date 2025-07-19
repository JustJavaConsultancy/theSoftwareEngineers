/**
 * Register an event at the document for the specified selector,
 * so events are still catched after DOM changes.
 */
function handleEvent(eventType, selector, handler) {
  document.addEventListener(eventType, function(event) {
    if (event.target.matches(selector + ', ' + selector + ' *')) {
      handler.apply(event.target.closest(selector), arguments);
    }
  });
}

handleEvent('click', '.js-file-delete', function(event) {
  const $fileDiv = event.target.parentElement;
  const $fileRow = $fileDiv.previousElementSibling;
  $fileRow.removeAttribute('disabled');
  $fileRow.classList.remove('d-none');
  $fileDiv.remove();
});

document.addEventListener('DOMContentLoaded', function() {
  const menuToggle = document.getElementById('menu-toggle');
  const sidebar = document.querySelector('.sidebar');
  const sidebarOverlay = document.querySelector('.sidebar-overlay');

  if (menuToggle && sidebar && sidebarOverlay) {
    menuToggle.addEventListener('click', function() {
      sidebar.classList.toggle('active');
      sidebarOverlay.classList.toggle('active');
    });

    sidebarOverlay.addEventListener('click', function() {
      sidebar.classList.remove('active');
      sidebarOverlay.classList.remove('active');
    });

    document.addEventListener('click', function(event) {
      const isClickInsideSidebar = sidebar.contains(event.target);
      const isClickOnMenuToggle = menuToggle.contains(event.target);

      if (!isClickInsideSidebar && !isClickOnMenuToggle && window.innerWidth < 768) {
        sidebar.classList.remove('active');
        sidebarOverlay.classList.remove('active');
      }
    });
  }

  // Handle dropdown functionality for actual dropdowns (not modal triggers)
   // Simple modal functionality
    function openProcessModal() {
      const modal = document.getElementById('processModal');
      modal.classList.remove('hidden', 'hide');
      modal.classList.add('show');
      document.body.style.overflow = 'hidden';
    }

    function closeProcessModal() {
      const modal = document.getElementById('processModal');
      modal.classList.remove('show');
      modal.classList.add('hide');
      // Wait for animation to finish before hiding
      modal.addEventListener('animationend', function handleAnimationEnd() {
        modal.classList.add('hidden');
        modal.classList.remove('hide');
        modal.removeEventListener('animationend', handleAnimationEnd);
      });
      document.body.style.overflow = 'auto';
    }

    document.querySelectorAll('.dropdown-toggle').forEach(button => {
          button.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            openProcessModal();
          });
        });

        // Handle close button
        const closeButton = document.getElementById('closeModal');
        if (closeButton) {
          closeButton.addEventListener('click', closeProcessModal);
        }

        // Close modal when clicking backdrop
        const modal = document.getElementById('processModal');
        if (modal) {
          modal.addEventListener('click', function(e) {
            if (e.target === modal) {
              closeProcessModal();
            }
          });
        }

        // Close modal with Escape key
        document.addEventListener('keydown', function(e) {
          if (e.key === 'Escape') {
            const modal = document.getElementById('processModal');
            if (modal && !modal.classList.contains('hidden')) {
              closeProcessModal();
            }
          }
        });
});
