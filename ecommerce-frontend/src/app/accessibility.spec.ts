import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { App } from './app';
import { routes } from './app.routes';

describe('Route accessibility', () => {
  it('focuses the new route heading and keeps loading announcements outside busy forms', async () => {
    TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter(routes), provideHttpClient()],
    });
    const fixture = TestBed.createComponent(App);
    document.body.appendChild(fixture.nativeElement);
    fixture.detectChanges();
    for (const path of ['/auth/login', '/auth/register']) {
      await TestBed.inject(Router).navigateByUrl(path);
      await fixture.whenStable();
      const root: HTMLElement = fixture.nativeElement;
      expect(root.querySelectorAll('main')).toHaveLength(1);
      const heading = root.querySelector('h1');
      expect(document.activeElement).toBe(heading);
      expect(heading?.getAttribute('tabindex')).toBe('-1');
      const status = root.querySelector('[role="status"]');
      expect(status?.closest('[aria-busy]')).toBeNull();
      for (const input of root.querySelectorAll('input')) {
        expect(root.querySelector(`label[for="${input.id}"]`)).not.toBeNull();
      }
    }
    fixture.nativeElement.remove();
  });
});
