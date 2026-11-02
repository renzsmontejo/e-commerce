import { afterNextRender, Component, ElementRef, inject, Injector } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  constructor() {
    const host = inject<ElementRef<HTMLElement>>(ElementRef);
    const injector = inject(Injector);
    inject(Router)
      .events.pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => {
        afterNextRender(
          () => {
            const heading = host.nativeElement.querySelector<HTMLElement>('h1');
            heading?.setAttribute('tabindex', '-1');
            heading?.focus({ preventScroll: true });
          },
          { injector },
        );
      });
  }
}
