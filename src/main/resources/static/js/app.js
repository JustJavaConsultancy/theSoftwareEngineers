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
            const dropdownToggles = document.querySelectorAll('.dropdown-toggle');

                        dropdownToggles.forEach(toggle => {
                            toggle.addEventListener('click', function(e) {
                                e.stopPropagation();
                                const dropdownMenu = this.nextElementSibling;
                                const isOpen = dropdownMenu.classList.contains('show');

                                // Close all other open dropdowns
                                document.querySelectorAll('.dropdown-menu.show').forEach(menu => {
                                    if (menu !== dropdownMenu) {
                                        menu.classList.remove('show');
                                    }
                                });

                                // Toggle current dropdown
                                dropdownMenu.classList.toggle('show', !isOpen);
                            });
                        });

                        // Close dropdowns when clicking outside
                        document.addEventListener('click', function() {
                            document.querySelectorAll('.dropdown-menu.show').forEach(menu => {
                                menu.classList.remove('show');
                            });
                        });

                        // Close dropdowns when pressing Escape key
                        document.addEventListener('keydown', function(e) {
                            if (e.key === 'Escape') {
                                document.querySelectorAll('.dropdown-menu.show').forEach(menu => {
                                    menu.classList.remove('show');
                                });
                            }
                        });

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

                            document.querySelectorAll('.process-modal-toggle').forEach(button => {
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
                                })

            // Dark/Light mode toggle logic
            const modeToggleBtn = document.getElementById('mode-toggle');
            const body = document.body;
            const MODE_KEY = 'colorMode';
            const ICON_MOON = 'nightlight_round';
            const ICON_SUN = 'wb_sunny';

            function setMode(mode) {
                if (mode === 'light') {
                    body.classList.add('light-mode');
                } else {
                    body.classList.remove('light-mode');
                }
                // Change icon if possible
                if (modeToggleBtn) {
                    const icon = modeToggleBtn.querySelector('.material-icons');
                    if (icon) {
                        icon.textContent = mode === 'light' ? ICON_SUN : ICON_MOON;
                    }
                }
            }

            // On load, apply saved mode
            const savedMode = localStorage.getItem(MODE_KEY);
            if (savedMode === 'light' || savedMode === 'dark') {
                setMode(savedMode);
            }

            // Toggle on button click
            if (modeToggleBtn) {
                modeToggleBtn.addEventListener('click', function() {
                    const isLight = body.classList.toggle('light-mode');
                    const newMode = isLight ? 'light' : 'dark';
                    localStorage.setItem(MODE_KEY, newMode);
                    setMode(newMode);
                });
            }
        });